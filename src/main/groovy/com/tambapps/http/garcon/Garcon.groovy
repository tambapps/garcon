package com.tambapps.http.garcon

import com.tambapps.http.garcon.exception.RequestParsingException
import com.tambapps.http.garcon.io.RequestParser
import groovy.transform.TupleConstructor
import jdk.nashorn.internal.ir.annotations.Immutable
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
  private final Queue<Closeable> connections = new ConcurrentLinkedQueue<>()
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
      connections.add(serverSocket)
      while (running.get()) {
        Socket socket = serverSocket.accept()
        connections.add(socket)
        requestsExecutorService.submit(new RequestHandler(socket))
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

  private EndpointDefinition getMatchingEndpointDefinition(String p) {
    def path = Paths.get(p)
    return endpointDefinitions.find { Paths.get(it.path) == path }
  }

  @TupleConstructor
  private class RequestHandler implements Runnable {

    Socket socket

    @Override
    void run() {
      try {
        InputStream inputStream = socket.inputStream
        OutputStream outputStream = socket.outputStream
        String connectionHeader = CONNECTION_KEEP_ALIVE
        while (running && connectionHeader.equalsIgnoreCase(CONNECTION_KEEP_ALIVE)) {
          HttpRequest request
          try {
            request = requestParser.parse(inputStream)
          } catch (RequestParsingException e) {
            newResponse(400, 'Bad Request', CONNECTION_CLOSE, 'Request is malformed'.bytes).writeInto(outputStream)
            continue
          }
          connectionHeader = request.headers[CONNECTION_HEADER] ?: CONNECTION_CLOSE
          String responseConnectionHeader = connectionHeader.equalsIgnoreCase(CONNECTION_KEEP_ALIVE) ? CONNECTION_KEEP_ALIVE : CONNECTION_CLOSE

          EndpointDefinition endpointDefinition = getMatchingEndpointDefinition(request.path)
          if (endpointDefinition.method != request.method) {
            newResponse(405, 'Method Not Allowed', CONNECTION_CLOSE,
                "Method ${request.method} is not allowed at this path".bytes).writeInto(outputStream)
            continue
          }
          HttpResponse response
          if (endpointDefinition != null) {
            response = newResponse(200, 'Ok', responseConnectionHeader, null)
            endpointDefinition.closure.resolveStrategy = Closure.DELEGATE_FIRST
            // TODO doesn't work
            endpointDefinition.closure.delegate = new Context(garcon: Garcon.this, request: request, response: response)
            try {
              Object returnValue = endpointDefinition.closure(request)
              if (response.body == null && returnValue != null) {
                switch (returnValue) {
                  case byte[]:
                    response.body = (byte[]) returnValue
                    break
                  case String:
                    response.body = ((String) returnValue).bytes
                    break
                }
              }
            } catch (Exception e) {
              e.printStackTrace()
              response = newResponse(500, 'Internal Server Error', responseConnectionHeader, 'An internal server error occurred'.bytes)
            }
          } else {
            byte[] responseBody = "Resource at path ${request.path} was not found".bytes
            response = newResponse(404, 'Not Found', responseConnectionHeader, responseBody)
          }
          if (response.body != null) {
            response.headers['Content-Length'] = response.body.size()
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

    private HttpResponse newResponse(int status, String message, String connection, byte[] body) {
      def headers = [Connection: connection,
                     Server: 'Garcon (Tambapps)',
                     Date: 'Mon, 23 May 2005 22:38:34 GMT']
      if (body != null) {
        headers.putAll([
            ('Content-Length'): body.size().toString()
        ])
      }
      return new HttpResponse(httpVersion: 'HTTP/1.1', statusCode: status, message: message,
          headers: new Headers(headers),
          body: body)
    }
  }


  @Immutable
  class Context {
    HttpRequest request
    HttpResponse response
    Garcon garcon
    // TODO
  }
}
