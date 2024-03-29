package com.tambapps.http.garcon

import com.tambapps.http.garcon.annotation.Endpoint
import com.tambapps.http.garcon.annotation.PathVariable
import com.tambapps.http.garcon.annotation.QueryParam
import com.tambapps.http.garcon.annotation.RequestHeader
import com.tambapps.http.garcon.annotation.ResponseStatus
import com.tambapps.http.hyperpoet.ErrorResponseException
import com.tambapps.http.hyperpoet.interceptor.ConsolePrintingInterceptor

import static com.tambapps.http.garcon.ContentType.CONTENT_TYPE_JSON
import com.tambapps.http.garcon.annotation.Get
import com.tambapps.http.garcon.annotation.ParsedRequestBody
import com.tambapps.http.garcon.annotation.Post
import com.tambapps.http.hyperpoet.HttpPoet
import groovy.transform.CompileDynamic
import okhttp3.OkHttpClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import java.time.Duration

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertThrows

// needed because project is compiled statically by default
@CompileDynamic
class GarconInstanceTest {

  private final HttpPoet poet = new HttpPoet(url: 'http://localhost:8081').tap {
    configureOkHttpClient { OkHttpClient.Builder builder -> builder.readTimeout(Duration.ofMillis(10_000))}
    addInterceptor(new ConsolePrintingInterceptor())
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
    assertEquals(HttpStatus.PARTIAL_CONTENT.value, poet.history.last().responseCode)
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

  @Test
  void testQueryParams() {
    assertEquals('p 0', poet.get('/qp?p=p'))
    assertEquals('p 25', poet.get('/qp?p=p&count=25'))

    ErrorResponseException e = assertThrows(ErrorResponseException) { poet.get('/qp') }
    assertEquals(400, e.code)
  }

  @Test
  void testHeaders() {
    assertEquals('h 0', poet.get('/h', headers: [h: 'h']))
    assertEquals('h 25', poet.get('/h', headers: [h: 'h', cOuNt: 25]))

    ErrorResponseException e = assertThrows(ErrorResponseException) { poet.get('/h') }
    assertEquals(400, e.code)
  }

  @Test
  void testPathVariable() {
    assertEquals('1', poet.get('/user/1'))

    ErrorResponseException e = assertThrows(ErrorResponseException) { poet.get('/user/notAnInt') }
    assertEquals(400, e.code)
  }

  @Test
  void testObjectBody() {
    assertEquals('magic', poet.post('/objectBody',
        body: [bar: 'magic'], contentType: com.tambapps.http.hyperpoet.ContentType.JSON))

    ErrorResponseException e = assertThrows(ErrorResponseException) { poet.post('/objectBody',
        body: [barrrrr: 'magic'], contentType: com.tambapps.http.hyperpoet.ContentType.JSON) }
    assertEquals(400, e.code)
    assertEquals('Unknown property barrrrr', e.body.string())
  }

  @ResponseStatus(HttpStatus.PARTIAL_CONTENT)
  @Get("/hello")
  def getHello() {
    return 'Hello World'
  }

  @Endpoint(method = "GET", path = "/hello2")
  def getHelloWho(HttpExchangeContext context) {
    return context.queryParams['who']
  }

  @Get("/qp")
  def getQueryParam(@QueryParam("p") String p, @QueryParam(name = "count", required = false, defaultValue = "0") Integer count) {
    return "$p $count"
  }

  @Get("/user/{id}")
  def getUser(@PathVariable("id") Integer id) {
    return "$id"
  }

  @Get("/h")
  def getHeader(@RequestHeader("H") String h, @RequestHeader(name = "count", required = false, defaultValue = "0") Integer count) {
    return "$h $count"
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
  void postMirror3(@ParsedRequestBody Map<?, ?> requestBody, HttpResponse response) {
    response.body = requestBody.who
  }

  @Post(path = "/objectBody", accept = CONTENT_TYPE_JSON)
  void postMirror4(@ParsedRequestBody Foo requestBody, HttpResponse response) {
    response.body = requestBody.bar
  }

  static class Foo {
    String bar
  }
}
