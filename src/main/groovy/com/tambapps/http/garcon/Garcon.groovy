package com.tambapps.http.garcon

import com.tambapps.http.garcon.exception.RequestParsingException
import com.tambapps.http.garcon.io.RequestParser
import com.tambapps.http.garcon.io.composer.Composers
import com.tambapps.http.garcon.util.ContentTypeMap
import groovy.transform.TupleConstructor
import org.codehaus.groovy.runtime.DefaultGroovyMethods

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
  private final EndpointsHandler endpointsHandler = new EndpointsHandler()
  final ContentTypeMap<Closure<?>> composers = Composers.map
  int nbThreads = 4
  // TODO use me
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

          EndpointDefinition endpointDefinition = endpointsHandler.getMatchingEndpointDefinition(request.path)
          if (endpointDefinition.method != request.method) {
            newResponse(405, 'Method Not Allowed', CONNECTION_CLOSE,
                "Method ${request.method} is not allowed at this path".bytes).writeInto(outputStream)
            continue
          }
          HttpResponse response
          if (endpointDefinition != null) {
            response = newResponse(200, 'Ok', responseConnectionHeader, null)
            endpointDefinition.rehydrate(new Context(request, response, composers))
            try {
              Object returnValue = endpointDefinition.call()
              if (response.body == null && returnValue != null) {
                ContentType contentType = endpointDefinition.contentType ?: Garcon.this.contentType
                if (contentType != null) {
                  def composer = composers[contentType]
                  if (composer) {
                    returnValue = composer.call(returnValue)
                  }
                }
                response.body = returnValue
              }
            } catch (Exception e) {
              e.printStackTrace()
              responseConnectionHeader = CONNECTION_CLOSE
              connectionHeader = CONNECTION_CLOSE
              response = newResponse(500, 'Internal Server Error', responseConnectionHeader, 'An internal server error occurred'.bytes)
            }
          } else {
            byte[] responseBody = "Resource at path ${request.path} was not found".bytes
            response = newResponse(404, 'Not Found', responseConnectionHeader, responseBody)
          }
          if (response.indefiniteSize) {
            response.headers['Connection'] = CONNECTION_CLOSE
            connectionHeader = CONNECTION_CLOSE
          } else {
            response.headers['Content-Length'] = response.contentSize
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
      Map headers = [Connection: connection,
                     Server: 'Garcon (Tambapps)',
                     Date: 'Mon, 23 May 2005 22:38:34 GMT']
      if (body != null) {
        headers['Content-Length'] = body.size()
      }
      return new HttpResponse(httpVersion: 'HTTP/1.1', statusCode: status, message: message,
          headers: new Headers(headers),
          body: body)
    }
  }


  /**
   * Context used for endpoint definition closures, as delegate
   */
  static class Context {
    Context(HttpRequest request, HttpResponse response, ContentTypeMap<Closure<?>> composers) {
      this.request = request
      this.response = response
      this.composers = composers
    }
    // definition order matters because of @delegate
    @Delegate
    final HttpResponse response
    @Delegate
    final HttpRequest request
    private final ContentTypeMap<Closure<?>> composers

    Headers getRequestHeaders() {
      return request.headers
    }
    Headers getResponseHeaders() {
      return response.headers
    }

    // called when method is not found
    def invokeMethod(String name, Object o) {
      Object[] args = o instanceof Object[] ? o : new Object[] { o }
      if (args.length == 1) {
        def contentType = composers.keySet().find { it.subtype == name }
        if (contentType != null) {
          return composers[contentType].call(args[0])
        }
      }
      throw new MissingMethodException(name, getClass(), args)
    }
  }
}
