package com.tambapps.http.garcon;

import com.tambapps.http.garcon.io.parser.HttpRequestParser;

import java.nio.ByteBuffer;

public class HttpAttachment {
  private final HttpRequestParser requestParser = new HttpRequestParser();

  public HttpRequest parseRequest(ByteBuffer buffer) {
    return requestParser.parse(buffer) ? requestParser.getRequest() : null;
  }

  public void reset() {
    requestParser.reset();
  }
}
