package com.tambapps.http.garcon.exception;

/**
 * Exception to use when user made a bad request
 */
public class BadRequestException extends RuntimeException {
  /**
   * Construct a Bad Request Exception with the provided message
   * @param message the message
   */
  public BadRequestException(String message) {
    super(message);
  }
}
