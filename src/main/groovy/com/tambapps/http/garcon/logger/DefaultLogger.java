package com.tambapps.http.garcon.logger;

import java.util.Date;

/**
 * A default implementation of a {@link Logger}
 */
public class DefaultLogger implements Logger {

  private static final String ERROR = "ERROR";
  @Override
  public void error(String message) {
    log(ERROR, message);
  }

  @Override
  public void error(String message, Throwable e) {
    log(ERROR, message);
    if (e != null) {
      e.printStackTrace();
    } else {
      System.out.println("null");
    }
  }

  private static void log(String level, String message) {
    System.out.format("%s [%s] %s - %s", new Date(), level, Thread.currentThread()
        .getName(), message).println();
  }
}
