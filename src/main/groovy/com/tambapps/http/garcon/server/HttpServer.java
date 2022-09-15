package com.tambapps.http.garcon.server;

import java.net.InetAddress;

public interface HttpServer {

  /**
   * Starts the server on the given address and port. The server starts in a background thread,
   * meaning this method does not block to wait for the server to stop.
   *
   * @param address the address to use
   * @param port    the port to use
   * @return true if the server was successfully started
   */
  boolean start(InetAddress address, Integer port);

  /**
   * Stops the server
   */
  void stop();

  /**
   * Return true if the server is running
   * @return true if the server is running
   */
  boolean isRunning();

  /**
   * Wait for the server to stop
   */
  void join();
}
