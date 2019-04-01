// Copyright 2013 Square, Inc.
package retrofit;

import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Response;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import retrofit.http.GET;
import rx.Observable;
import rx.Observer;

import static com.squareup.okhttp.Protocol.HTTP_1_1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class ErrorHandlerTest {

  interface ExampleClient {
    @GET("/")
    Response throwsCustomException() throws TestException;

    @GET("/")
    void onErrorWrappedCustomException(Callback<Response> callback);

    @GET("/")
    Observable<Response> onErrorCustomException();
  }

  static class TestException extends Exception {
  }

  ExampleClient example;
  ErrorHandler errorHandler;

  @Before public void setup() {
    errorHandler = mock(ErrorHandler.class);

    OkHttpClient client = new OkHttpClient();
    client.interceptors().add(new Interceptor() {
      @Override public Response intercept(Chain chain) throws IOException {
        return new Response.Builder()
            .code(400)
            .message("Invalid")
            .request(chain.request())
            .protocol(HTTP_1_1)
            .build();
      }
    });

    example = new RestAdapter.Builder() //
        .setEndpoint("http://example.com")
        .setClient(client)
        .setErrorHandler(errorHandler)
        .setCallbackExecutor(new Utils.SynchronousExecutor())
        .build()
        .create(ExampleClient.class);
  }

  @Test public void customizedExceptionUsed() throws Throwable {
    TestException exception = new TestException();
    doReturn(exception).when(errorHandler).handleError(any(RetrofitError.class));

    try {
      example.throwsCustomException();
      failBecauseExceptionWasNotThrown(TestException.class);
    } catch (TestException e) {
      assertThat(e).isSameAs(exception);
    }
  }

  @Test public void onErrorWrappedCustomException() throws Throwable {
    final TestException exception = new TestException();
    doReturn(exception).when(errorHandler).handleError(any(RetrofitError.class));

    example.onErrorWrappedCustomException(new Callback<Response>() {

      @Override public void success(Response response, Response response2) {
        failBecauseExceptionWasNotThrown(TestException.class);
      }

      @Override public void failure(RetrofitError error) {
        assertThat(error.getCause()).isSameAs(exception);
      }
    });
  }

  @Test public void onErrorCustomException() throws Throwable {
    final TestException exception = new TestException();
    doReturn(exception).when(errorHandler).handleError(any(RetrofitError.class));

    example.onErrorCustomException().subscribe(new Observer<Response>() {
      @Override public void onCompleted() {
        failBecauseExceptionWasNotThrown(TestException.class);
      }

      @Override public void onError(Throwable e) {
        assertThat(e).isSameAs(exception);
      }

      @Override public void onNext(Response response) {
        failBecauseExceptionWasNotThrown(TestException.class);
      }
    });
  }

  @Test public void returningNullThrowsException() throws Exception {
    doReturn(null).when(errorHandler).handleError(any(RetrofitError.class));

    try {
      example.throwsCustomException();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage()).isEqualTo("Error handler returned null for wrapped exception.");
    }
  }
}
