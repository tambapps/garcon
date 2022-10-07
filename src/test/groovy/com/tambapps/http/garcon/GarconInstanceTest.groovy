package com.tambapps.http.garcon

import static com.tambapps.http.garcon.ContentType.CONTENT_TYPE_JSON
import com.tambapps.http.garcon.annotation.Get
import com.tambapps.http.garcon.annotation.ParsedRequestBody
import com.tambapps.http.garcon.annotation.Post
import com.tambapps.http.hyperpoet.ErrorResponseHandlers
import com.tambapps.http.hyperpoet.HttpPoet
import groovy.transform.CompileDynamic
import okhttp3.OkHttpClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import java.time.Duration

import static org.junit.jupiter.api.Assertions.assertEquals

// needed because project is compiled statically by default
@CompileDynamic
class GarconInstanceTest {

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
  private final Garcon garcon = Garcon.fromInstance(this).tap {
    address = "localhost"
    port = 8081
    onServerError = { Exception e -> e.printStackTrace() }
    onExchangeError = { Exception e -> e.printStackTrace() }
  }
  private boolean firstCall

  @BeforeEach
  void init() {
    garcon.start()
    firstCall = true
  }

  @AfterEach
  void dispose() {
    garcon.stop()
  }

  @Test
  void test() {
    assertEquals('Hello World', poet.get('/hello'))
    assertEquals('Hello World', poet.get('/hello?p=1&a=2'))
    assertEquals('Hello World', poet.get('/hello/?p=1&a=2'))

    assertEquals('me', poet.get('/hello2?who=me'))
    assertEquals('nobody', poet.get('/hello2?who=nobody'))
  }

  @Test
  void testMirror() {
    assertEquals('magic', poet.post('/mirror', body: 'magic'))
    assertEquals('magic', poet.post('/mirror2', body: 'magic'))

    assertEquals('magic', poet.post('/mirror3', body: [who: 'magic'], contentType: com.tambapps.http.hyperpoet.ContentType.JSON))
  }


  @Get("/hello")
  def getHello() {
    return 'Hello World'
  }

  @Get("/hello2")
  def getHelloWho(HttpExchangeContext context) {
    return context.queryParams['who']
  }

  @Post("/mirror")
  def postMirror(HttpRequest request) {
    return new String(request.body)
  }

  @Post("/mirror2")
  void postMirror2(HttpRequest request, HttpResponse response) {
    response.body = request.body
  }

  @Post(path = "/mirror3", accept = CONTENT_TYPE_JSON)
  void postMirror2(@ParsedRequestBody Map<?, ?> requestBody, HttpResponse response) {
    response.body = requestBody.who
  }
}
