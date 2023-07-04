package com.tambapps.http.garcon.exception;

public class ParsingException extends BadRequestException {
  public ParsingException(Throwable cause) {
    super("Request body is malformed", cause);
  }
}
