package com.tambapps.http.garcon

import com.tambapps.http.hyperpoet.ErrorResponseException
import com.tambapps.http.hyperpoet.ErrorResponseHandlers
import com.tambapps.http.hyperpoet.HttpPoet
import com.tambapps.http.hyperpoet.io.parser.Parsers
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.CompileDynamic
import okhttp3.OkHttpClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

import java.time.Duration
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue

// needed because project is compiled statically by default
@CompileDynamic
class GarconTest {

  private final HttpPoet poet = new HttpPoet(url: 'http://localhost:8081').tap {
    errorResponseHandler = ErrorResponseHandlers.parseResponseHandler(it)
    configureOkHttpClient { OkHttpClient.Builder builder -> builder.readTimeout(Duration.ofMillis(10_000))}
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
    garcon = new Garcon(InetAddress.getByName("localhost"), 8081).tap {
      onServerError = { Exception e -> e.printStackTrace() }
      onExchangeError = { Exception e -> e.printStackTrace() }
    }
    firstCall = true
  }

  @AfterEach
  void dispose() {
    garcon.stop()
  }

  @Test
  void test() {
    garcon.serve {
      get 'hello', {
        return 'Hello World'
      }
    }

    assertEquals('Hello World', poet.get('/hello'))
    assertEquals('Hello World', poet.get('/hello?p=1&a=2'))
    assertEquals('Hello World', poet.get('/hello/?p=1&a=2'))
  }

  @Test
  void testDynamic() {
    garcon.serve {
      get 'hello/{someone}', {
        return pathVariables.someone
      }

      get 'hello2/{someone}', {
        return someone
      }
    }

    assertEquals('me', poet.get('/hello/me'))
    assertEquals('you', poet.get('/hello/you'))
    assertEquals('yall', poet.get('/hello/yall/'))

    assertEquals('me', poet.get('/hello2/me'))
    assertEquals('you', poet.get('/hello2/you'))
    assertEquals('yall', poet.get('/hello2/yall/'))
  }

  @Test
  void testQueryParams() {
    garcon.serve {
      get '/path', {
        return queryParams['hello']
      }

      get '/path2', {
        body = queryParams['hello'].bytes
      }

      get '/path3', {
        body = hello
      }
    }

    assertEquals('world', poet.get('/path?hello=world'))
    assertEquals('world', poet.get('/path2?hello=world'))
    assertEquals('world', poet.get('/path3?hello=world'))
  }

  @Test
  void testResponseBodyTypes() {
    def responseBody = 'Hello World'
    garcon.serve {
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
    garcon.serve {
      get '/hello', {
        return 'Hello World'
      }
    }

    poet.delete('/hello')
    assertEquals(405, poet.history.last().responseCode)
  }

  @Test
  void testStatus() {
    garcon.serve {
      get '/hello', {
        statusCode = HttpStatus.CREATED
        return 'Hello World'
      }
    }

    assertEquals('Hello World', poet.get('/hello'))
    assertEquals(201, poet.history.last().responseCode)
  }

  @Test
  void testContentType() {
    garcon.serve {
      get '/path', contentType: ContentType.JSON, {
        return [hello: 'world']
      }
      // should also work with hyperpoet content type
      get '/path2', contentType: com.tambapps.http.hyperpoet.ContentType.JSON, {
        return [hello: 'world']
      }
    }

    assertEquals($/{"hello":"world"}/$, poet.get('/path', parser: Parsers.&parseStringResponseBody))
    assertEquals(ContentType.JSON.headerValue, poet.history.last().responseHeaders[Headers.CONTENT_TYPE_HEADER].first())
    assertEquals($/{"hello":"world"}/$, poet.get('/path2', parser: Parsers.&parseStringResponseBody))
    assertEquals(ContentType.JSON.headerValue, poet.history.last().responseHeaders[Headers.CONTENT_TYPE_HEADER].first())
  }

  @Test
  void testOverrideParser() {
    AtomicBoolean wasHere = new AtomicBoolean(false)
    garcon.parsers[ContentType.JSON] = { byte[] bytes ->
      wasHere.set(true)
      new JsonSlurper().parse(bytes)
    }
    garcon.serve {
      post '/path', accept: ContentType.JSON, contentType: ContentType.JSON, {
        return parsedRequestBody
      }
    }

    assertEquals($/{"hello":"world"}/$, poet.post('/path', body: [hello: 'world'], contentType: com.tambapps.http.hyperpoet.ContentType.JSON,
        parser: Parsers.&parseStringResponseBody))
    assertTrue(wasHere.get())
  }

  @Test
  void testAccept() {
    garcon.serve {
      post '/path', accept: ContentType.JSON, {
        parsedRequestBody
        return 'yes'
      }
    }
    assertEquals('yes', poet.post('/path', body: [hello: 'world'], contentType: com.tambapps.http.hyperpoet.ContentType.JSON))
  }

  @Test
  void testParseBadJson() {
    garcon.serve {
      post '/path', accept: ContentType.JSON, {
        parsedRequestBody
        return [hello: 'world']
      }
      post '/path2', {
        getParsedRequestBody(ContentType.JSON)
        return [hello: 'world']
      }
    }

    poet.post('/path', body: 'not json')
    assertEquals(400, poet.history.last().responseCode)
    poet.post('/path2', body: 'not json')
    assertEquals(400, poet.history.last().responseCode)
  }

  @Test
  void testDefaultContentType() {
    garcon.serve {
      contentType = ContentType.JSON
      get '/path', {
      return [hello: 'world']
      }
    }
    assertEquals($/{"hello":"world"}/$, poet.get('/path', parser: Parsers.&parseStringResponseBody))
  }

  @Disabled
  @Test
  void testBenchmark() {
    garcon.serve {
      post '/path', accept: ContentType.JSON, contentType: ContentType.JSON, {
        response.setBody(JsonOutput.toJson(parsedRequestBody))
      }
    }
    Thread.sleep(1000L)
    int totalRequests = 50_000
    int nbThreads = 50
    ExecutorService executorService = Executors.newFixedThreadPool(nbThreads)

    println("Starting threads")
    final int requestsPerThreads = totalRequests / nbThreads
    List<Future> futures = []
    poet.disableHistory()
    poet.errorResponseHandler = ErrorResponseHandlers.throwResponseHandler()
    Collection collection = new ConcurrentLinkedQueue()
    for (_ in 0..<nbThreads) {
      def f = executorService.submit {
        for (i in 0..<requestsPerThreads) {
          try {
            collection << poet.post('/path', contentType: com.tambapps.http.hyperpoet.ContentType.JSON, body: [id: 1, data: 'lol'])
          } catch (ErrorResponseException e) {
            e.printStackTrace()
            println(e.getBody().string())
          } catch (Exception e) {
            e.printStackTrace()
          }
        }
      }
      futures << f
    }
    futures*.get()
    println(collection.groupBy { it }.collectEntries {[(it.key): it.value.size()]})
    executorService.shutdown()
  }
}
