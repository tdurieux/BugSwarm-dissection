/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package okhttp3.internal.huc;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.SocketPermission;
import java.net.URL;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Handshake;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.Internal;
import okhttp3.internal.JavaNetHeaders;
import okhttp3.internal.Platform;
import okhttp3.internal.URLFilter;
import okhttp3.internal.Util;
import okhttp3.internal.Version;
import okhttp3.internal.http.HttpDate;
import okhttp3.internal.http.HttpEngine;
import okhttp3.internal.http.HttpMethod;
import okhttp3.internal.http.OkHeaders;
import okhttp3.internal.http.StatusLine;

import static okhttp3.internal.Platform.WARN;

/**
 * This implementation uses {@linkplain Call} to send requests and receive responses.
 *
 * <h3>What does 'connected' mean?</h3> This class inherits a {@code connected} field from the
 * superclass. That field is <strong>not</strong> used to indicate whether this URLConnection is
 * currently connected. Instead, it indicates whether a connection has ever been attempted. Once a
 * connection has been attempted, certain properties (request header fields, request method, etc.)
 * are immutable.
 */
public class HttpURLConnectionImpl extends HttpURLConnection implements Callback {
  private static final Set<String> METHODS = new LinkedHashSet<>(
      Arrays.asList("OPTIONS", "GET", "HEAD", "POST", "PUT", "DELETE", "TRACE", "PATCH"));

  OkHttpClient client;

  Headers.Builder requestHeaders = new Headers.Builder();

  /** Like the superclass field of the same name, but a long and available on all platforms. */
  long fixedContentLength = -1L;
  IOException callFailure;
  Call call;
  Response response;
  Response networkResponse;
  /** Lazily created (with synthetic headers) on first call to getHeaders(). */
  Headers responseHeaders;

  URLFilter urlFilter;

  boolean executed;
  boolean connectPending = true;
  Proxy proxy;
  Handshake handshake;
  final NetworkInterceptor networkInterceptor = new NetworkInterceptor();

  public HttpURLConnectionImpl(URL url, OkHttpClient client) {
    super(url);
    this.client = client;
  }

  public HttpURLConnectionImpl(URL url, OkHttpClient client, URLFilter urlFilter) {
    this(url, client);
    this.urlFilter = urlFilter;
  }

  @Override public final void connect() throws IOException {
    if (executed) return;

    Call call = buildCall();
    executed = true;
    call.enqueue(this);

    synchronized (this) {
      try {
        while (connectPending && response == null && callFailure == null) {
          wait();
        }
        if (callFailure != null) {
          throw callFailure;
        }
      } catch (InterruptedException e) {
        throw new InterruptedIOException();
      }
    }
  }

  @Override public final void disconnect() {
    // Calling disconnect() before a connection exists should have no effect.
    if (call == null) return;

    networkInterceptor.proceed(); // Unblock any waiting async thread.
    call.cancel();
  }

  /**
   * Returns an input stream from the server in the case of error such as the requested file (txt,
   * htm, html) is not found on the remote server.
   */
  @Override public final InputStream getErrorStream() {
    try {
      Response response = getResponse();
      if (HttpEngine.hasBody(response) && response.code() >= HTTP_BAD_REQUEST) {
        return response.body().byteStream();
      }
      return null;
    } catch (IOException e) {
      return null;
    }
  }

  private Headers getHeaders() throws IOException {
    if (responseHeaders == null) {
      Response response = getResponse();
      Headers headers = response.headers();
      responseHeaders = headers.newBuilder()
          .add(OkHeaders.SELECTED_PROTOCOL, response.protocol().toString())
          .add(OkHeaders.RESPONSE_SOURCE, responseSourceHeader(response))
          .build();
    }
    return responseHeaders;
  }

  private static String responseSourceHeader(Response response) {
    if (response.networkResponse() == null) {
      if (response.cacheResponse() == null) {
        return "NONE";
      }
      return "CACHE " + response.code();
    }
    if (response.cacheResponse() == null) {
      return "NETWORK " + response.code();
    }
    return "CONDITIONAL_CACHE " + response.networkResponse().code();
  }

  /**
   * Returns the value of the field at {@code position}. Returns null if there are fewer than {@code
   * position} headers.
   */
  @Override public final String getHeaderField(int position) {
    try {
      Headers headers = getHeaders();
      if (position < 0 || position >= headers.size()) return null;
      return headers.value(position);
    } catch (IOException e) {
      return null;
    }
  }

  /**
   * Returns the value of the field corresponding to the {@code fieldName}, or null if there is no
   * such field. If the field has multiple values, the last value is returned.
   */
  @Override public final String getHeaderField(String fieldName) {
    try {
      return fieldName == null
          ? StatusLine.get(getResponse()).toString()
          : getHeaders().get(fieldName);
    } catch (IOException e) {
      return null;
    }
  }

  @Override public final String getHeaderFieldKey(int position) {
    try {
      Headers headers = getHeaders();
      if (position < 0 || position >= headers.size()) return null;
      return headers.name(position);
    } catch (IOException e) {
      return null;
    }
  }

  @Override public final Map<String, List<String>> getHeaderFields() {
    try {
      return JavaNetHeaders.toMultimap(getHeaders(),
          StatusLine.get(getResponse()).toString());
    } catch (IOException e) {
      return Collections.emptyMap();
    }
  }

  @Override public final Map<String, List<String>> getRequestProperties() {
    if (connected) {
      throw new IllegalStateException(
          "Cannot access request header fields after connection is set");
    }

    return JavaNetHeaders.toMultimap(requestHeaders.build(), null);
  }

  @Override public final InputStream getInputStream() throws IOException {
    if (!doInput) {
      throw new ProtocolException("This protocol does not support input");
    }

    Response response = getResponse();

    if (response.code() >= HTTP_BAD_REQUEST) {
      throw new FileNotFoundException(url.toString());
    }

    return response.body().byteStream();
  }

  @Override public final OutputStream getOutputStream() throws IOException {
    OutputStreamRequestBody requestBody = (OutputStreamRequestBody) buildCall().request().body();
    if (requestBody == null) {
      throw new ProtocolException("method does not support a request body: " + method);
    }

    // If this request needs to stream bytes to the server, build a physical connection immediately
    // and start streaming those bytes over that connection.
    if (requestBody instanceof StreamedRequestBody) {
      connect();
      networkInterceptor.proceed();
    } else if (executed) {
      throw new ProtocolException("cannot write request body after response has been read");
    }

    return requestBody.outputStream();
  }

  @Override public final Permission getPermission() throws IOException {
    URL url = getURL();
    String hostname = url.getHost();
    int hostPort = url.getPort() != -1
        ? url.getPort()
        : HttpUrl.defaultPort(url.getProtocol());
    if (usingProxy()) {
      InetSocketAddress proxyAddress = (InetSocketAddress) client.proxy().address();
      hostname = proxyAddress.getHostName();
      hostPort = proxyAddress.getPort();
    }
    return new SocketPermission(hostname + ":" + hostPort, "connect, resolve");
  }

  @Override public final String getRequestProperty(String field) {
    if (field == null) return null;
    return requestHeaders.get(field);
  }

  @Override public void setConnectTimeout(int timeoutMillis) {
    client = client.newBuilder()
        .connectTimeout(timeoutMillis, TimeUnit.MILLISECONDS)
        .build();
  }

  @Override
  public void setInstanceFollowRedirects(boolean followRedirects) {
    client = client.newBuilder()
        .followRedirects(followRedirects)
        .build();
  }

  @Override public boolean getInstanceFollowRedirects() {
    return client.followRedirects();
  }

  @Override public int getConnectTimeout() {
    return client.connectTimeoutMillis();
  }

  @Override public void setReadTimeout(int timeoutMillis) {
    client = client.newBuilder()
        .readTimeout(timeoutMillis, TimeUnit.MILLISECONDS)
        .build();
  }

  @Override public int getReadTimeout() {
    return client.readTimeoutMillis();
  }

  private Call buildCall() throws IOException {
    if (call != null) {
      return call;
    }

    connected = true;
    if (doOutput) {
      if (method.equals("GET")) {
        // they are requesting a stream to write to. This implies a POST method
        method = "POST";
      } else if (!HttpMethod.permitsRequestBody(method)) {
        throw new ProtocolException(method + " does not support writing");
      }
    }

    if (requestHeaders.get("User-Agent") == null) {
      requestHeaders.add("User-Agent", defaultUserAgent());
    }

    OutputStreamRequestBody requestBody = null;
    if (HttpMethod.permitsRequestBody(method)) {
      // Add a content type for the request body, if one isn't already present.
      String contentType = requestHeaders.get("Content-Type");
      if (contentType == null) {
        contentType = "application/x-www-form-urlencoded";
        requestHeaders.add("Content-Type", contentType);
      }

      boolean stream = fixedContentLength != -1L || chunkLength > 0;

      long contentLength = -1L;
      String contentLengthString = requestHeaders.get("Content-Length");
      if (fixedContentLength != -1L) {
        contentLength = fixedContentLength;
      } else if (contentLengthString != null) {
        contentLength = Long.parseLong(contentLengthString);
      }

      requestBody = stream
          ? new StreamedRequestBody(contentLength)
          : new BufferedRequestBody(contentLength);
      requestBody.timeout().timeout(client.writeTimeoutMillis(), TimeUnit.MILLISECONDS);
    }

    Request request = new Request.Builder()
        .url(Internal.instance.getHttpUrlChecked(getURL().toString()))
        .headers(requestHeaders.build())
        .method(method, requestBody)
        .build();

    if (urlFilter != null) {
      urlFilter.checkURLPermitted(request.url().url());
    }

    OkHttpClient.Builder clientBuilder = client.newBuilder();
    clientBuilder.interceptors().clear();
    clientBuilder.networkInterceptors().clear();
    clientBuilder.networkInterceptors().add(networkInterceptor);

    // If we're currently not using caches, make sure the engine's client doesn't have one.
    if (!getUseCaches()) {
      clientBuilder.cache(null);
    }

    return call = clientBuilder.build().newCall(request);
  }

  private String defaultUserAgent() {
    String agent = System.getProperty("http.agent");
    return agent != null ? Util.toHumanReadableAscii(agent) : Version.userAgent();
  }

  /**
   * Aggressively tries to get the final HTTP response, potentially making many HTTP requests in the
   * process in order to cope with redirects and authentication.
   */
  private Response getResponse() throws IOException {
    if (response != null) {
      return response;
    } else if (networkResponse != null) {
      return networkResponse;
    } else if (callFailure != null) {
      throw callFailure;
    }

    Call call = buildCall();
    networkInterceptor.proceed();

    OutputStreamRequestBody requestBody = (OutputStreamRequestBody) call.request().body();
    if (requestBody != null) requestBody.outputStream().close();

    if (executed) {
      synchronized (this) {
        try {
          while (response == null && callFailure == null) {
            wait();
          }
        } catch (InterruptedException e) {
          throw new InterruptedIOException();
        }
      }
    } else {
      executed = true;
      try {
        onResponse(call, call.execute());
      } catch (IOException e) {
        onFailure(call, e);
      }
    }

    if (callFailure != null) throw callFailure;
    if (response != null) return response;
    throw new AssertionError();
  }

  /**
   * Returns true if either:
   *
   * <ul>
   *   <li>A specific proxy was explicitly configured for this connection.
   *   <li>The response has already been retrieved, and a proxy was {@link
   *       java.net.ProxySelector selected} in order to get it.
   * </ul>
   *
   * <p><strong>Warning:</strong> This method may return false before attempting to connect and true
   * afterwards.
   */
  @Override public final boolean usingProxy() {
    if (proxy != null) return true;
    Proxy clientProxy = client.proxy();
    return clientProxy != null && clientProxy.type() != Proxy.Type.DIRECT;
  }

  @Override public String getResponseMessage() throws IOException {
    return getResponse().message();
  }

  @Override public final int getResponseCode() throws IOException {
    return getResponse().code();
  }

  @Override public final void setRequestProperty(String field, String newValue) {
    if (connected) {
      throw new IllegalStateException("Cannot set request property after connection is made");
    }
    if (field == null) {
      throw new NullPointerException("field == null");
    }
    if (newValue == null) {
      // Silently ignore null header values for backwards compatibility with older
      // android versions as well as with other URLConnection implementations.
      //
      // Some implementations send a malformed HTTP header when faced with
      // such requests, we respect the spec and ignore the header.
      Platform.get().log(WARN, "Ignoring header " + field + " because its value was null.", null);
      return;
    }

    // TODO: Deprecate use of X-Android-Transports header?
    if ("X-Android-Transports".equals(field) || "X-Android-Protocols".equals(field)) {
      setProtocols(newValue, false /* append */);
    } else {
      requestHeaders.set(field, newValue);
    }
  }

  @Override public void setIfModifiedSince(long newValue) {
    super.setIfModifiedSince(newValue);
    if (ifModifiedSince != 0) {
      requestHeaders.set("If-Modified-Since", HttpDate.format(new Date(ifModifiedSince)));
    } else {
      requestHeaders.removeAll("If-Modified-Since");
    }
  }

  @Override public final void addRequestProperty(String field, String value) {
    if (connected) {
      throw new IllegalStateException("Cannot add request property after connection is made");
    }
    if (field == null) {
      throw new NullPointerException("field == null");
    }
    if (value == null) {
      // Silently ignore null header values for backwards compatibility with older
      // android versions as well as with other URLConnection implementations.
      //
      // Some implementations send a malformed HTTP header when faced with
      // such requests, we respect the spec and ignore the header.
      Platform.get().log(WARN, "Ignoring header " + field + " because its value was null.", null);
      return;
    }

    // TODO: Deprecate use of X-Android-Transports header?
    if ("X-Android-Transports".equals(field) || "X-Android-Protocols".equals(field)) {
      setProtocols(value, true /* append */);
    } else {
      requestHeaders.add(field, value);
    }
  }

  /*
   * Splits and validates a comma-separated string of protocols.
   * When append == false, we require that the transport list contains "http/1.1".
   * Throws {@link IllegalStateException} when one of the protocols isn't
   * defined in {@link Protocol OkHttp's protocol enumeration}.
   */
  private void setProtocols(String protocolsString, boolean append) {
    List<Protocol> protocolsList = new ArrayList<>();
    if (append) {
      protocolsList.addAll(client.protocols());
    }
    for (String protocol : protocolsString.split(",", -1)) {
      try {
        protocolsList.add(Protocol.get(protocol));
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
    client = client.newBuilder()
        .protocols(protocolsList)
        .build();
  }

  @Override public void setRequestMethod(String method) throws ProtocolException {
    if (!METHODS.contains(method)) {
      throw new ProtocolException("Expected one of " + METHODS + " but was " + method);
    }
    this.method = method;
  }

  @Override public void setFixedLengthStreamingMode(int contentLength) {
    setFixedLengthStreamingMode((long) contentLength);
  }

  @Override public void setFixedLengthStreamingMode(long contentLength) {
    if (super.connected) throw new IllegalStateException("Already connected");
    if (chunkLength > 0) throw new IllegalStateException("Already in chunked mode");
    if (contentLength < 0) throw new IllegalArgumentException("contentLength < 0");
    this.fixedContentLength = contentLength;
    super.fixedContentLength = (int) Math.min(contentLength, Integer.MAX_VALUE);
  }

  @Override public synchronized void onFailure(Call call, IOException e) {
    this.callFailure = e;
    notifyAll();
  }

  @Override public synchronized void onResponse(Call call, Response response) {
    this.response = response;
    this.handshake = response.handshake();
    this.url = response.request().url().url();
    notifyAll();
  }

  final class NetworkInterceptor implements Interceptor {
    private boolean proceed;

    public void proceed() {
      synchronized (HttpURLConnectionImpl.this) {
        this.proceed = true;
        HttpURLConnectionImpl.this.notifyAll();
      }
    }

    @Override public Response intercept(Chain chain) throws IOException {
      synchronized (HttpURLConnectionImpl.this) {
        connectPending = false;
        proxy = chain.connection().route().proxy();
        handshake = chain.connection().handshake();
        HttpURLConnectionImpl.this.notifyAll();

        try {
          while (!proceed) {
            HttpURLConnectionImpl.this.wait();
          }
        } catch (InterruptedException e) {
          throw new InterruptedIOException();
        }
      }

      Response response = chain.proceed(chain.request());

      synchronized (HttpURLConnectionImpl.this) {
        networkResponse = response;
        url = response.request().url().url();
      }

      return response;
    }
  }
}
