package com.tambapps.http.garcon.logger;

public interface Logger {

  void error(String message);
  void error(String message, Throwable e);


}
