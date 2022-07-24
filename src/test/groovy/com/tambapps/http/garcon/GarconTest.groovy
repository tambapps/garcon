package com.tambapps.http.garcon

import com.tambapps.http.hyperpoet.ErrorResponseHandlers
import com.tambapps.http.hyperpoet.HttpPoet
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals

class GarconTest {

  private final HttpPoet poet = new HttpPoet(url: 'http://localhost:8081').tap {
    errorResponseHandler = ErrorResponseHandlers.parseResponseHandler(it)
    enableHistory(1)
    onPreExecute = {
      // to give time garcon to start
      if (this.firstCall) {
        Thread.sleep(100)
        this.firstCall = false
      }
    }
  }
  private Garcon garcon
  private boolean firstCall

  @BeforeEach
  void init() {
    garcon = new Garcon()
    firstCall = true
  }

  @AfterEach
  void dispose() {
    garcon.stop()
  }

  @Test
  void test() {
    garcon.serveAsync {
      get '/hello', {
        return 'Hello World'
      }
    }

    assertEquals('Hello World', poet.get('/hello'))
    assertEquals('Hello World', poet.get('/hello?p=1&a=2'))
  }
  @Test
  void testMethodNotAllowed() {
    garcon.serveAsync {
      get '/hello', {
        return 'Hello World'
      }
    }

    poet.errorResponseHandler = ErrorResponseHandlers.parseResponseHandler(poet)
    poet.delete('/hello')
    assertEquals(405, poet.history.last().responseCode)
  }

}
