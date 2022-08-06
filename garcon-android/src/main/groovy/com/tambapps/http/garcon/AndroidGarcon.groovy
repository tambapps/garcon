package com.tambapps.http.garcon

import com.tambapps.http.garcon.io.composer.Composers
import com.tambapps.http.garcon.io.composer.Parsers
import com.tambapps.http.garcon.util.ContentTypeMap
import com.tambapps.http.garcon.util.IoUtils
import groovy.transform.CompileStatic
import groovy.transform.PackageScope

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@CompileStatic
class AndroidGarcon extends AbstractGarcon {

  final ContentTypeMap<Closure<?>> composers = Composers.map
  final ContentTypeMap<Closure<?>> parsers = Parsers.map

  private final AtomicBoolean running = new AtomicBoolean(false)
  private final Queue<Closeable> connections = new ConcurrentLinkedQueue<>()
  private ExecutorService requestsExecutorService

  AndroidGarcon() {}

  AndroidGarcon(String address, int port) {
    this((InetAddress) InetAddress.getByName(address), port)
  }

  AndroidGarcon(InetAddress address, int port) {
    super()
    super.setAddress(address)
    super.setPort(port)
  }

  AndroidGarcon(InetAddress address, int port, int backlog) {
    super()
    super.setAddress(address)
    super.setPort(port)
    super.setBacklog(backlog)
  }

  @Override
  void doStart(EndpointsHandler endpointsHandler) {
    if (running.get()) {
      // already running
      return
    }
    running.set(true)
    requestsExecutorService = Executors.newFixedThreadPool(nbThreads)
    try (ServerSocket serverSocket = newServerSocket()) {
      connections.add(serverSocket)
      onStarted(serverSocket.getInetAddress(), serverSocket.getLocalPort())
      while (running.get()) {
        Socket socket = serverSocket.accept()
        socket.setSoTimeout(requestReadTimeoutMillis)
        connections.add(socket)
        requestsExecutorService.submit(new HttpExchangeHandler(socket, this, endpointsHandler, connections))
      }
    } catch (SocketException e) {
      // the socket was probably closed, do nothing
      onServerSocketClosed(e)
    } catch (IOException e) {
      onServerException(e)
    }
    running.set(false)
  }

  protected void onStarted(InetAddress address, int port) {
    this.onStarted?.call(address, port)
  }

  @PackageScope
  void onServerSocketClosed(SocketException e) {
    onClosed?.call()
  }

  @PackageScope
  void onServerException(IOException e) {
    onError?.call(e)
  }

  private ServerSocket newServerSocket() {
    int port = this.port != null ? this.port : 0
    int backlog = this.backlog != null ? this.backlog : 0
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

  @Override
  boolean isRunning() {
    return running.get()
  }

  @Override
  void doStop() {
    running.set(false)
    if (requestsExecutorService != null) {
      requestsExecutorService.shutdown()
    }
    requestsExecutorService = null

    for (Closeable connection : connections) {
      IoUtils.closeQuietly(connection)
    }
    connections.clear()
  }
}
