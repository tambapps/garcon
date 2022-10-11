package com.tambapps.http.garcon.endpoint

import com.tambapps.http.garcon.exception.MethodNotAllowedException
import com.tambapps.http.garcon.exception.NotFoundException
import groovy.transform.CompileDynamic
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue

@CompileDynamic
class DynamicEndpointsHandlerTest {

  private static final EndpointDefinition DEFINITION_1 = new DynamicEndpointDefinition(null, null, null, null, null)
  private static final EndpointDefinition DEFINITION_2 = new DynamicEndpointDefinition(null, null, null, null, null)
  private static final EndpointDefinition DEFINITION_3 = new DynamicEndpointDefinition(null, null, null, null, null)

  @Test
  void testDynamic() {
    DynamicEndpointsHandler endpointsHandler = new DynamicEndpointsHandler()
    assertTrue(endpointsHandler.isEmpty())

    endpointsHandler.defineEndpoint('/path/{foo}', 'POST', DEFINITION_1)
    endpointsHandler.defineEndpoint('/path/{foo}/path2/{bar}', 'GET', DEFINITION_2)
    assertThrows(IllegalStateException) {
      endpointsHandler.defineEndpoint('/path/{foo}/path2/{bar}', 'GET', DEFINITION_3)
    }
    assertFalse(endpointsHandler.isEmpty())

    assertEquals(DEFINITION_1, endpointsHandler.getEndpoint('/path/ca', 'POST'))
    assertEquals(DEFINITION_1, endpointsHandler.getEndpoint('/path/1234', 'POST'))

    assertThrows(NotFoundException) {
      endpointsHandler.getEndpoint('/bar', 'GET')
    }
    assertThrows(MethodNotAllowedException) {
      endpointsHandler.getEndpoint('/path/id', 'DELETE')
    }
  }

  @Test
  void testStatic() {
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

    assertThrows(NotFoundException) {
      endpointsHandler.getEndpoint('/bar', 'GET')
    }
    assertThrows(MethodNotAllowedException) {
      endpointsHandler.getEndpoint('/foo', 'DELETE')
    }
  }
}
