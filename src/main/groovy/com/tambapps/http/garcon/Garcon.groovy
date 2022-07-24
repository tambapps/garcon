package com.tambapps.http.garcon

import com.tambapps.http.garcon.io.RequestParser
import groovy.transform.TupleConstructor
import org.codehaus.groovy.runtime.DefaultGroovyMethods

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class Garcon {

  private static final String CONNECTION_KEEP_ALIVE = 'keep-alive'
  private static final String CONNECTION_CLOSE = 'close'
  private static final String CONNECTION_HEADER = 'Connection'
  private ExecutorService executorService
  private final AtomicBoolean running = new AtomicBoolean(false)
  private final RequestParser requestParser = new RequestParser()
  private final ExecutorService requestsExecutorService = Executors.newFixedThreadPool(4)

  void start() {
    running.set(true)
    try {
      ServerSocket serverSocket = new ServerSocket(8081, 2, InetAddress.getByName('localhost'))
      while (running.get()) {
        Socket socket = serverSocket.accept()
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

          HttpResponse response = new HttpResponse(httpVersion: 'HTTP/1.1', statusCode: 200, message: 'OK',
              headers: [Connection: 'close', ('Content-Length'): 'Hello World'.bytes.size().toString(),
                        Date: 'Mon, 23 May 2005 22:38:34 GMT'],
              body: 'Hello World'.bytes)
          response.writeInto(outputStream)
        }
      } catch (EOFException e) {
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
      }
    }
  }
}
