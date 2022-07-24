package com.tambapps.http.garcon

import com.tambapps.http.garcon.io.RequestParser

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class Garcon {

  private ExecutorService executorService
  private final AtomicBoolean shouldStop = new AtomicBoolean(false)
  private final AtomicBoolean running = new AtomicBoolean(false)
  private final RequestParser requestParser = new RequestParser()


  void start() {
    shouldStop.set(false)
    running.set(true)
    try {
      ServerSocket serverSocket = new ServerSocket(8081, 2, InetAddress.getByName('localhost'))
      while (!shouldStop.get()) {
        Socket socket = serverSocket.accept()
        // TODO handle socket in different threads
        try (InputStream inputStream = socket.inputStream
        OutputStream outputStream = socket.outputStream) {
          HttpRequest request = requestParser.parse(inputStream)
          // TODO handle Connection: keep-alive
          HttpResponse response = new HttpResponse(httpVersion: 'HTTP/1.1', statusCode: 200, message: 'OK',
              headers: [Connection: 'close', ('Content-Length'): 'Hello World'.bytes.size().toString(),
              Date: 'Mon, 23 May 2005 22:38:34 GMT'],
              body: 'Hello World'.bytes)
          response.writeInto(outputStream)
        } catch (IOException e) {
          e.printStackTrace()
          // TODO
        } finally {
          socket.close()
        }
      }
    } catch (IOException e) {
      e.printStackTrace()
      // TODO
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
    shouldStop.set(true)
  }

}
