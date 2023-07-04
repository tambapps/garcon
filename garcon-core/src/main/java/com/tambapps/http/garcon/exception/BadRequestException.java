package com.tambapps.http.garcon.exception;

import com.tambapps.http.garcon.HttpStatus;

/**
 * Exception to use when user made a bad request
 */
public class BadRequestException extends HttpStatusException {
  public BadRequestException(String message, Throwable cause) {
    super(message, cause, HttpStatus.BAD_REQUEST);
  }

  /**
   * Construct a Bad Request Exception with the provided message
   * @param message the message
   */
  public BadRequestException(String message) {
    super(message, HttpStatus.BAD_REQUEST);
  }
}
