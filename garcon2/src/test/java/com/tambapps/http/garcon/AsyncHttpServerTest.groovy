package com.tambapps.http.garcon

import com.tambapps.http.hyperpoet.ErrorResponseHandlers
import com.tambapps.http.hyperpoet.HttpPoet
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import java.time.Duration
import java.util.concurrent.Executors

import static org.junit.jupiter.api.Assertions.assertEquals

class AsyncHttpServerTest {

  AsyncHttpServer httpServer
  private final HttpPoet poet = new HttpPoet(url: 'http://localhost:8081').tap {
    errorResponseHandler = ErrorResponseHandlers.parseResponseHandler(it)
    configureOkHttpClient { it.readTimeout(Duration.ofMillis(10_000))}
    enableHistory(1)
    onPreExecute = {
      // to give time garcon to start
      if (this.firstCall) {
        Thread.sleep(100)
        this.firstCall = false
      }
    }
  }
  private boolean firstCall


  @BeforeEach
  void begin() {
    httpServer = new AsyncHttpServer(Executors.newFixedThreadPool(4), {
      return new HttpResponse()
    })
    if (!httpServer.start(InetAddress.getByName('localhost'), 8081)) {
      throw new RuntimeException("Couldn't start the server")
    }
  }

  @AfterEach
  void stop() {
    httpServer.stop()
  }


  @Test
  void test() {
    httpServer.setExchangeHandler {
      def response = new HttpResponse()
      response.body = "${it.method} ${it.path} " + it.queryParams['hello']
      return response
    }

    assertEquals('GET /path world', poet.get('/path?hello=world'))
  }
}
