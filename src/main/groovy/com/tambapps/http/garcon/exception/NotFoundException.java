package com.tambapps.http.garcon.exception;

import com.tambapps.http.garcon.HttpStatus;

public class NotFoundException extends HttpStatusException {

  public NotFoundException(String message) {
    super(message, HttpStatus.NOT_FOUND);
  }

  public NotFoundException(String message, Throwable cause) {
    super(message, cause, HttpStatus.NOT_FOUND);
  }

}
