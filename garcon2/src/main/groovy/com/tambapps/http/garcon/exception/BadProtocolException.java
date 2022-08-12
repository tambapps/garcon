package com.tambapps.http.garcon.exception;

public class BadProtocolException extends RuntimeException {
  public BadProtocolException() {
  }

  public BadProtocolException(String message, Throwable cause) {
    super(message, cause);
  }

  public BadProtocolException(String message) {
    super(message);
  }
}
