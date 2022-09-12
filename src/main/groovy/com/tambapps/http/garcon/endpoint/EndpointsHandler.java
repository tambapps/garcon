package com.tambapps.http.garcon.endpoint;

import com.tambapps.http.garcon.exception.MethodNotAllowedException;
import com.tambapps.http.garcon.exception.PathNotFoundException;

/**
 * Endpoint handler
 */
public interface EndpointsHandler {

  EndpointDefinition getEndpoint(String path, String method) throws PathNotFoundException,
      MethodNotAllowedException;

  boolean isEmpty();

}
