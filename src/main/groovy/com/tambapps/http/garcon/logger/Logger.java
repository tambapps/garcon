package com.tambapps.http.garcon.logger;

public interface Logger {

  /**
   * Logs an error message
   *
   * @param message the message
   */
  void error(String message);

  /**
   * Logs an error message
   *
   * @param message the message
   * @param e       the throwable
   */
  void error(String message, Throwable e);


}
