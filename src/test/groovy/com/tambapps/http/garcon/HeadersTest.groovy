package com.tambapps.http.garcon

import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue

class HeadersTest {

  @Test
  void testHeaders() {
    def h = new Headers()
    h['Connection'] = 'keep-alive'
    assertTrue(h.containsKey('connection'))

    h['cOnNeCtion'] = 'Close'
    assertEquals(1, h.size())
    assertEquals('Close', h['connection'])
  }

  @Test
  void testImmutableHeaders() {
    def h = new ImmutableHeaders([name: 'value'])
    assertThrows(UnsupportedOperationException) {
      h['name2'] = 'something'
    }
  }

}
