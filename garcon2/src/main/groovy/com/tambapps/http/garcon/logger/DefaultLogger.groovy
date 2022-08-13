package com.tambapps.http.garcon.logger

class DefaultLogger implements Logger {

  private static final String ERROR = 'ERROR'
  @Override
  void error(String message) {
    log(ERROR, message)
  }

  @Override
  void error(String message, Throwable e) {
    log(ERROR, message)
    e?.printStackTrace()
  }

  private static void log(String level, String message) {
    println(String.format("%s [%s] %s - %s", new Date(), level, Thread.currentThread()
        .getName(), message))
  }
}
