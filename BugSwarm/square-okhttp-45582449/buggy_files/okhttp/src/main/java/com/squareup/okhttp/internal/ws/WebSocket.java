/*
 * Copyright (C) 2014 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.okhttp.internal.ws;

import java.io.IOException;
import okio.Buffer;
import okio.BufferedSink;

// TODO move to public API!
/** Blocking interface to connect and write to a web socket. */
public interface WebSocket {
  /** The format of a message payload. */
  enum PayloadType {
    /** UTF8-encoded text data. */
    TEXT,
    /** Arbitrary binary data. */
    BINARY
  }

  /**
   * Stream a message payload to the server of the specified {code type}.
   * <p>
   * You must call {@link BufferedSink#close() close()} to complete the message. Calls to
   * {@link BufferedSink#flush() flush()} write a frame fragment. The message may be empty.
   *
   * @throws IllegalStateException if not connected, already closed, or another writer is active.
   */
  BufferedSink newMessageSink(RealWebSocket.PayloadType type);

  /**
   * Send a message payload the server of the specified {@code type}.
   *
   * @throws IllegalStateException if not connected, already closed, or another writer is active.
   */
  void sendMessage(RealWebSocket.PayloadType type, Buffer payload) throws IOException;

  void sendPing(Buffer payload) throws IOException;

  /**
   * Send a close frame to the server.
   * <p>
   * The corresponding {@link WebSocketListener} will continue to get messages until its
   * {@link WebSocketListener#onClose onClose()} method is called.
   * <p>
   * It is an error to call this method before calling close on an active writer. Calling this
   * method more than once has no effect.
   */
  void close(int code, String reason) throws IOException;

  /**
   * True if this web socket is closed and can no longer be written to.
   * <p>
   * Note: Due to the asynchronous nature of a websocket, a {@code true} value from method does not
   * guarantee that the connection will be open or even that you will be able to write in a
   * subsequent call.
   */
  boolean isClosed();
}
