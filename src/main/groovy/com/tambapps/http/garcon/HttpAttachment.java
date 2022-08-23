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

  public HttpRequest parseRequest(ByteBuffer buffer) {
    if (System.currentTimeMillis() - attachedAtMillis > timeoutMillis) {
      throw new RequestTimeoutException();
    }
    return requestParser.parse(buffer) ? requestParser.getRequest() : null;
  }

  public void reset() {
    requestParser.reset();
  }
}
