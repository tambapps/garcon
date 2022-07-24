package com.tambapps.http.garcon

import com.tambapps.http.hyperpoet.HttpPoet
import org.junit.jupiter.api.Test

class GarconTest {


  @Test
  void test() {
    println(InetAddress.getByName('localhost'))
    Garcon garcon = new Garcon()
    garcon.startAsync()
    Thread.sleep(500L)
    def poet = new HttpPoet(url: 'http://localhost:8081')
    println(poet.get('/'))
    garcon.stop()
  }

}
