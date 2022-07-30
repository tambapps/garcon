package com.tambapps.http.garcon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

public class HeadersTest {

  @Test
  public void testHeaders() {
    Headers h = new Headers();
    h.put("Connection", "keep-alive");
    assertTrue(h.containsKey("connection"));

    h.put("cOnNeCtion", "Close");
    assertEquals(1, h.size());
    assertEquals("Close", h.getAt("connection"));
  }

  @Test
  public void testImmutableHeaders() {
    ImmutableHeaders h = new ImmutableHeaders(Collections.singletonMap("name", "value"));
    assertThrows(UnsupportedOperationException.class, () -> h.putAt("name2", "something"));
  }

}
