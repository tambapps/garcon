package com.tambapps.http.garcon

import com.tambapps.http.garcon.io.composer.Composers
import com.tambapps.http.garcon.util.ContentTypeMap
import org.codehaus.groovy.runtime.DefaultGroovyMethods

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class Garcon {

  static final String CONNECTION_KEEP_ALIVE = 'Keep-Alive'
  static final String CONNECTION_CLOSE = 'Close'
  static final String CONNECTION_HEADER = 'Connection'

  private ExecutorService executorService
  private final AtomicBoolean running = new AtomicBoolean(false)
  private final Queue<Closeable> connections = new ConcurrentLinkedQueue<>()
  private ExecutorService requestsExecutorService
  private final EndpointsHandler endpointsHandler = new EndpointsHandler()
  final ContentTypeMap<Closure<?>> composers = Composers.map
  int nbThreads = 4
  ContentType accept
  ContentType contentType

  void define(@DelegatesTo(EndpointsHandler) Closure closure) {
    endpointsHandler.define(this, closure)
  }

  void serve(@DelegatesTo(EndpointDefiner) Closure closure) {
    define(closure)
    start()
  }

  void serveAsync(@DelegatesTo(EndpointDefiner) Closure closure) {
    define(closure)
    startAsync()
  }

  void start() {
    running.set(true)
    requestsExecutorService = Executors.newFixedThreadPool(nbThreads)
    try {
      ServerSocket serverSocket = new ServerSocket(8081, 2, InetAddress.getByName('localhost'))
      connections.add(serverSocket)
      while (running.get()) {
        Socket socket = serverSocket.accept()
        connections.add(socket)
        requestsExecutorService.submit(new HttpExchangeHandler(socket, this, endpointsHandler, connections))
      }
    } catch (SocketException e) {
      // the socket was probably closed, do nothing
    } catch (IOException e) {
      e.printStackTrace()
    }
    running.set(false)
  }

  boolean isRunning() {
    return running.get()
  }

  void startAsync() {
    executorService ?= Executors.newSingleThreadExecutor()
    executorService.submit {
      try {
        start()
      } catch (Exception e) {
        e.printStackTrace()
        // TODO
      }
    }
  }

  void stop() {
    running.set(false)
    requestsExecutorService?.shutdown()
    requestsExecutorService = null
    executorService?.shutdown()
    executorService = null
    connections.each(DefaultGroovyMethods.&closeQuietly)
    connections.clear()
  }
}
