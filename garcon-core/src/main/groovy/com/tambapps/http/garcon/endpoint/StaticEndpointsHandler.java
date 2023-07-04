package com.tambapps.http.garcon.endpoint;

import com.tambapps.http.garcon.exception.MethodNotAllowedException;
import com.tambapps.http.garcon.exception.NotFoundException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class StaticEndpointsHandler<T> implements EndpointsHandler<T> {

  final Map<String, Map<String, EndpointDefinition<T>>> endpointDefinitions;

  StaticEndpointsHandler() {
    this(new HashMap<>());
  }

  @Override
  public EndpointDefinition<T> getEndpoint(String path, String method) throws NotFoundException, MethodNotAllowedException {
    if (path.endsWith("/")) {
      path = path.substring(0, path.length() - 1);
    }
    if (!path.startsWith("/")) {
      path = "/" + path;
    }
    Map<String, EndpointDefinition<T>> methodDefinitions = endpointDefinitions.get(path);
    if (methodDefinitions == null) {
      throw new NotFoundException("No resource were found at the provided path");
    }
    EndpointDefinition<T> endpointDefinition = methodDefinitions.get(method);
    if (endpointDefinition == null) {
      throw new MethodNotAllowedException();
    }
    return endpointDefinition;
  }

  public void defineEndpoint(String path, String method, EndpointDefinition<T> endpointDefinition) {
    Map<String, EndpointDefinition<T>> methodMap = endpointDefinitions.computeIfAbsent(path, k -> new HashMap<>());
    if (methodMap.containsKey(method)) {
      throw new IllegalStateException(String.format("Endpoint %s %s is already defined", method, path));
    }
    methodMap.put(method.toUpperCase(), endpointDefinition);
  }

  @Override
  public boolean isEmpty() {
    return endpointDefinitions.isEmpty();
  }

}
