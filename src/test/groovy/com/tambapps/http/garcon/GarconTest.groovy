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
  void testMethodContext() {
    garcon.serveAsync {
      get '/path', {
        return queryParams['hello']
      }

      get '/path2', {
        body = queryParams['hello'].bytes
      }
    }

    assertEquals('world', poet.get('/path?hello=world'))
    assertEquals('world', poet.get('/path?hello=world'))
  }

  @Test
  void testResponseBodyTypes() {
    def responseBody = 'Hello World'
    garcon.serveAsync {
      get '/string', {
        return responseBody
      }
      get '/bytes', {
        return responseBody.bytes
      }
      get '/input-stream', {
        return new ByteArrayInputStream(responseBody.bytes)
      }

      get '/unknown', {
        return new Object()
      }
    }

    assertEquals(responseBody, poet.get('/string'))
    assertEquals(responseBody, poet.get('/bytes'))
    assertEquals(responseBody, poet.get('/input-stream'))

    poet.get('/unknown')
    assertEquals(500, poet.history.last().responseCode)
  }

  @Test
  void testMethodNotAllowed() {
    garcon.serveAsync {
      get '/hello', {
        return 'Hello World'
      }
    }

    poet.delete('/hello')
    assertEquals(405, poet.history.last().responseCode)
  }

  @Test
  void testStatus() {
    garcon.serveAsync {
      get '/hello', {
        statusCode = 201
        return 'Hello World'
      }
    }

    assertEquals('Hello World', poet.get('/hello'))
    assertEquals(201, poet.history.last().responseCode)
  }

  @Test
  void testContentType() {
    garcon.serveAsync {
      get '/path', contentType: ContentType.JSON, {
        return [hello: 'world']
      }
      get '/path2', {
      return json(hello: 'world')
      }
    }

    assertEquals($/{"hello":"world"}/$, poet.get('/path'))
    assertEquals($/{"hello":"world"}/$, poet.get('/path2'))
  }

  @Test
  void testDefaultContentType() {
    garcon.serveAsync {
      contentType = ContentType.JSON
      get '/path', {
      return [hello: 'world']
      }
    }
    assertEquals($/{"hello":"world"}/$, poet.get('/path'))
  }
}
