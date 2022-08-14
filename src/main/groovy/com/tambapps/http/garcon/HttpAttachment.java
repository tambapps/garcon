package com.tambapps.http.garcon;

import com.tambapps.http.garcon.io.parser.HttpRequestParser;
import lombok.Getter;
import lombok.Setter;

import java.nio.ByteBuffer;

public class HttpAttachment {
  private final HttpRequestParser requestParser = new HttpRequestParser();

  @Getter
  @Setter
  private boolean pendingWrite;

  public HttpRequest parseRequest(ByteBuffer buffer) {
    return requestParser.parse(buffer) ? requestParser.getRequest() : null;
  }

  public void reset() {
    pendingWrite = false;
    requestParser.reset();
  }
}
