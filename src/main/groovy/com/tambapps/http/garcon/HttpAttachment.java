package com.tambapps.http.garcon;

import com.tambapps.http.garcon.exception.RequestTimeoutException;
import com.tambapps.http.garcon.io.parser.HttpRequestParser;
import lombok.RequiredArgsConstructor;

import java.nio.ByteBuffer;

@RequiredArgsConstructor
public class HttpAttachment {
  private final HttpRequestParser requestParser = new HttpRequestParser();

  private final long attachedAtMillis = System.currentTimeMillis();
  private final long timeoutMillis;

  /**
   * Parse the request from the buffer
   * @param buffer the buffer
   * @return the request if fully parsed, or null
   */
  public HttpRequest parseRequest(ByteBuffer buffer) {
    if (System.currentTimeMillis() - attachedAtMillis > timeoutMillis) {
      throw new RequestTimeoutException();
    }
    return requestParser.parse(buffer) ? requestParser.getRequest() : null;
  }

  /**
   * reset the attachment
   */
  public void reset() {
    requestParser.reset();
  }
}
