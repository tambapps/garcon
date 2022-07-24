package com.tambapps.http.garcon

import com.tambapps.http.hyperpoet.ErrorResponseHandlers
import com.tambapps.http.hyperpoet.HttpPoet
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals

class GarconTest {


  @Test
  void test() {
    println(InetAddress.getByName('localhost'))
    Garcon garcon = new Garcon()
    garcon.serveAsync {
      get '/hello', {
        return 'Hello World'
      }
    }
    garcon.startAsync()
    Thread.sleep(500L)
    def poet = new HttpPoet(url: 'http://localhost:8081/hello?p=1&a=2')
    poet.errorResponseHandler = ErrorResponseHandlers.parseResponseHandler(poet)
    assertEquals('Hello World', (poet.get('/')))
    assertEquals('Hello World', (poet.get('/')))
    garcon.stop()
  }

}
