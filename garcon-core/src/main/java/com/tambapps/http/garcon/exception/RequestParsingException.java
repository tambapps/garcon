package com.tambapps.http.garcon.exception;

import java.io.IOException;

public class RequestParsingException extends IOException {

  public RequestParsingException(String var1) {
    super(var1);
  }

  public RequestParsingException(String var1, Throwable var2) {
    super(var1, var2);
  }
}
