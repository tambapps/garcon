package com.tambapps.http.garcon.endpoint;

import com.tambapps.http.garcon.exception.MethodNotAllowedException;
import com.tambapps.http.garcon.exception.PathNotFoundException;
import lombok.AllArgsConstructor;

import java.util.Map;

@AllArgsConstructor
public class StaticEndpointsHandler implements EndpointsHandler {

  // TODO handle dynamic paths
  final Map<String, Map<String, EndpointDefinition>> endpointDefinitions;

  @Override
  public EndpointDefinition getEndpoint(String path, String method) throws PathNotFoundException, MethodNotAllowedException {
    if (path.endsWith("/")) {
      path = path.substring(0, path.length() - 1);
    }
    if (!path.startsWith("/")) {
      path = "/" + path;
    }
    Map<String, EndpointDefinition> methodDefinitions = endpointDefinitions.get(path);
    if (methodDefinitions == null) {
      throw new PathNotFoundException();
    }
    EndpointDefinition endpointDefinition = methodDefinitions.get(method);
    if (endpointDefinition == null) {
      throw new MethodNotAllowedException();
    }
    return endpointDefinition;
  }

}
