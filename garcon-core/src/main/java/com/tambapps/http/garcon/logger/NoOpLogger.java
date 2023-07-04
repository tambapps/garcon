package com.tambapps.http.garcon.logger;

public class NoOpLogger implements Logger {
  @Override
  public void error(String message) {

  }

  @Override
  public void error(String message, Throwable e) {

  }
}
