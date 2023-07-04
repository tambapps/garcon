package com.tambapps.http.garcon.endpoint;

import com.tambapps.http.garcon.exception.MethodNotAllowedException;
import com.tambapps.http.garcon.exception.NotFoundException;

/**
 * Endpoint handler
 */
public interface EndpointsHandler<T> {

  /**
   * Get the endpoint for the given path and method
   *
   * @param path   the path of the endpoint
   * @param method the method of the endpoint
   * @return the endpoint for the given path and method
   * @throws NotFoundException     if no endpoints exists for the given path
   * @throws MethodNotAllowedException if an endpoint exists for this path but not this method
   */
  EndpointDefinition<T> getEndpoint(String path, String method) throws NotFoundException,
      MethodNotAllowedException;

  /**
   * Returns true if no endpoints were defined
   *
   * @return true if no endpoints were defined
   */
  boolean isEmpty();

}
