package com.tambapps.http.garcon.exception;

import com.tambapps.http.garcon.HttpStatusCode;
import lombok.Getter;

public class HttpStatusException extends RuntimeException {

  @Getter
  private final HttpStatusCode statusCode;

  public HttpStatusException(String message, HttpStatusCode statusCode) {
    super(message);
    this.statusCode = statusCode;
  }

  public HttpStatusException(String message, Throwable cause, HttpStatusCode statusCode) {
    super(message, cause);
    this.statusCode = statusCode;
  }
}
