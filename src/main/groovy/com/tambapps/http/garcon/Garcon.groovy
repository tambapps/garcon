package com.tambapps.http.garcon

import com.tambapps.http.garcon.io.RequestParser
import groovy.transform.TupleConstructor
import org.codehaus.groovy.runtime.DefaultGroovyMethods

import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class Garcon {

  private static final String CONNECTION_KEEP_ALIVE = 'Keep-Alive'
  private static final String CONNECTION_CLOSE = 'Close'
  private static final String CONNECTION_HEADER = 'Connection'
  private ExecutorService executorService
  private final AtomicBoolean running = new AtomicBoolean(false)
  private final RequestParser requestParser = new RequestParser()
  private final Queue<Socket> connections = new ConcurrentLinkedQueue<>()
  private ExecutorService requestsExecutorService
  private final List<EndpointDefinition> endpointDefinitions = []
  int nbThreads = 4

  void define(@DelegatesTo(EndpointDefiner) Closure closure) {
    EndpointDefiner definer = new EndpointDefiner()
    closure.delegate = definer
    closure.resolveStrategy = Closure.DELEGATE_FIRST
    closure()
    endpointDefinitions.addAll(definer.endpointDefinitions)
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
      while (running.get()) {
        Socket socket = serverSocket.accept()
        connections.add(socket)
        requestsExecutorService.submit(new RequestHandler(socket))
      }
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

  private EndpointDefinition getMatchingEndpointDefinition(String method, String p) {
    def path = Paths.get(p)
    return endpointDefinitions.find { Paths.get(it.path) == path && it.method == method }
  }

  @TupleConstructor
  private class RequestHandler implements Runnable {

    Socket socket

    @Override
    void run() {
      try {
        InputStream inputStream = socket.inputStream
        OutputStream outputStream = socket.outputStream
        String connection = CONNECTION_KEEP_ALIVE
        while (running && connection.equalsIgnoreCase(CONNECTION_KEEP_ALIVE)) {
          HttpRequest request = requestParser.parse(inputStream)
          connection = request.headers[CONNECTION_HEADER] ?: CONNECTION_CLOSE

          EndpointDefinition endpointDefinition = getMatchingEndpointDefinition(request.method, request.path)
          HttpResponse response
          if (endpointDefinition != null) {
            byte[] responseBody = endpointDefinition.closure(request)
            response = new HttpResponse(httpVersion: 'HTTP/1.1', statusCode: 200, message: 'OK',
                headers: [Connection: connection.equalsIgnoreCase(CONNECTION_KEEP_ALIVE) ? CONNECTION_KEEP_ALIVE : CONNECTION_CLOSE,
                          ('Content-Length'): responseBody.size().toString(),
                          Date: 'Mon, 23 May 2005 22:38:34 GMT'],
                body: responseBody)
          } else {
            // TODO handle method not accepted
            byte[] responseBody = "Resource at path ${request.path} was not found".bytes
            response = new HttpResponse(httpVersion: 'HTTP/1.1', statusCode: 404, message: 'NOT FOUND',
                headers: [Connection: connection.equalsIgnoreCase(CONNECTION_KEEP_ALIVE) ? CONNECTION_KEEP_ALIVE : CONNECTION_CLOSE,
                          ('Content-Length'): responseBody.size().toString(),
                          Date: 'Mon, 23 May 2005 22:38:34 GMT'],
                body: responseBody)
          }
          response.writeInto(outputStream)
        }
      } catch (EOFException|SocketException e) {
        // do nothing
      } catch (IOException e) {
        // TODO handle errors
        e.printStackTrace()
      } catch (Exception e) {
        // TODO handle errors
        e.printStackTrace()
      } finally {
        // closing socket will also close InputStream and OutputStream
        DefaultGroovyMethods.closeQuietly(socket)
        connections.remove(socket)
      }
    }
  }


  class Context {
    // TODO
  }
}
