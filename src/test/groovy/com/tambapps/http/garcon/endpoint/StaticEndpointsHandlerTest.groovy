package com.tambapps.http.garcon.endpoint

import com.tambapps.http.garcon.exception.MethodNotAllowedException
import com.tambapps.http.garcon.exception.PathNotFoundException
import groovy.transform.CompileDynamic
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue

@CompileDynamic
class StaticEndpointsHandlerTest {

  private static final EndpointDefinition DEFINITION_1 = new EndpointDefinition(null, null, null)
  private static final EndpointDefinition DEFINITION_2 = new EndpointDefinition(null, null, null)
  private static final EndpointDefinition DEFINITION_3 = new EndpointDefinition(null, null, null)

  @Test
  void test() {
    StaticEndpointsHandler endpointsHandler = new StaticEndpointsHandler()
    assertTrue(endpointsHandler.isEmpty())

    endpointsHandler.defineEndpoint('/foo', 'POST', DEFINITION_1)
    endpointsHandler.defineEndpoint('/foo', 'GET', DEFINITION_2)
    assertThrows(IllegalStateException) {
      endpointsHandler.defineEndpoint('/foo', 'GET', DEFINITION_3)
    }
    assertFalse(endpointsHandler.isEmpty())

    assertEquals(DEFINITION_1, endpointsHandler.getEndpoint('/foo', 'POST'))
    assertEquals(DEFINITION_1, endpointsHandler.getEndpoint('foo', 'POST'))
    assertEquals(DEFINITION_1, endpointsHandler.getEndpoint('/foo/', 'POST'))
    assertEquals(DEFINITION_1, endpointsHandler.getEndpoint('foo/', 'POST'))

    assertEquals(DEFINITION_2, endpointsHandler.getEndpoint('/foo', 'GET'))
    assertEquals(DEFINITION_2, endpointsHandler.getEndpoint('foo', 'GET'))
    assertEquals(DEFINITION_2, endpointsHandler.getEndpoint('/foo/', 'GET'))
    assertEquals(DEFINITION_2, endpointsHandler.getEndpoint('foo/', 'GET'))

    assertThrows(PathNotFoundException) {
      endpointsHandler.getEndpoint('/bar', 'GET')
    }
    assertThrows(MethodNotAllowedException) {
      endpointsHandler.getEndpoint('/foo', 'DELETE')
    }
  }
}
