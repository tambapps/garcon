package com.tambapps.http.garcon.endpoint;

import com.tambapps.http.garcon.exception.MethodNotAllowedException;
import com.tambapps.http.garcon.exception.PathNotFoundException;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Endpoints handler that supports both static and dynamic paths, at the cost of taking more time
 * to find a specific endpoint
 */
public class DynamicEndpointsHandler extends StaticEndpointsHandler {
  final Map<Pattern, Map<String, EndpointDefinition>> dynamicEndpoints;

  DynamicEndpointsHandler(Map<String, Map<String, EndpointDefinition>> endpointDefinitions,
      Map<Pattern, Map<String, EndpointDefinition>> dynamicEndpoints) {
    super(endpointDefinitions);
    this.dynamicEndpoints = dynamicEndpoints;
  }

  @Override
  public EndpointDefinition getEndpoint(String path, String method)
      throws PathNotFoundException, MethodNotAllowedException {
    // TODO find path variables
    return super.getEndpoint(path, method);
  }
}
