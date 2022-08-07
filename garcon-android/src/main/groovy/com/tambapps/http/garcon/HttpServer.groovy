package com.tambapps.http.garcon

import com.tambapps.http.garcon.util.IoUtils

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class HttpServer {

  static HttpServer create(AndroidGarcon garcon, InetAddress address, int port, int backlog, int requestReadTimeoutMillis, int nbThreads) {
    return new HttpServer(garcon, address, port, backlog, requestReadTimeoutMillis, Executors.newFixedThreadPool(nbThreads))
  }

  private final Queue<Closeable> connections = new ConcurrentLinkedQueue<>()
  private final AtomicBoolean running = new AtomicBoolean(false)
  private final AndroidGarcon garcon
  private final InetAddress address
  private final int port
  private final int backlog
  private final int requestReadTimeoutMillis
  private final ExecutorService requestsExecutorService

  private HttpServer(AndroidGarcon garcon, InetAddress address, int port, int backlog, int requestReadTimeoutMillis, ExecutorService requestsExecutorService) {
    this.garcon = garcon
    this.address = address
    this.port = port
    this.backlog = backlog
    this.requestReadTimeoutMillis = requestReadTimeoutMillis
    this.requestsExecutorService = requestsExecutorService
  }

  void run() {
    if (running.get()) {
      // already running
      return
    }
    running.set(true)
    try (ServerSocket serverSocket = newServerSocket()) {
      connections.add(serverSocket)
      garcon.onStarted(serverSocket.getInetAddress(), serverSocket.getLocalPort())
      while (running.get()) {
        Socket socket = serverSocket.accept()
        socket.setSoTimeout(requestReadTimeoutMillis)
        connections.add(socket)
        requestsExecutorService.submit(new AndroidHttpExchangeHandler(garcon: garcon, socket: socket, connections: connections, endpointsHandler: garcon.endpointsHandler))
      }
    } catch (SocketException e) {
      // the socket was probably closed, do nothing
      garcon.onServerSocketClosed(e)
    } catch (IOException e) {
      garcon.onServerException(e)
    }
    running.set(false)
  }

  private ServerSocket newServerSocket() {
    try {
      if (address != null) {
        return new ServerSocket(port, backlog, address)
      } else {
        return new ServerSocket(port, backlog)
      }
    } catch (IOException e) {
      throw new RuntimeException("Couldn't start server socket: " + e.getMessage(), e)
    }
  }

  boolean isRunning() {
    return running.get()
  }

  void stop() {
    running.set(false)
    for (Closeable connection : connections) {
      IoUtils.closeQuietly(connection)
    }
    connections.clear()
  }
}
