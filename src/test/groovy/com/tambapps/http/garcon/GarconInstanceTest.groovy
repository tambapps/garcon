package com.tambapps.http.garcon

import com.tambapps.http.garcon.annotation.Get
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
  }

  @Test
  void testHello2() {
    assertEquals('me', poet.get('/hello2?who=me'))
    assertEquals('nobody', poet.get('/hello2?who=nobody'))
  }


  @Get("/hello")
  def getHello() {
    return 'Hello World'
  }

  @Get("/hello2")
  def getHelloWho(HttpExchangeContext context) {
    return context.queryParams['who']
  }
}
