package com.tambapps.http.garcon;

import com.tambapps.http.garcon.exception.MethodNotAllowedException;
import com.tambapps.http.garcon.exception.PathNotFoundException;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;

import java.util.HashMap;
import java.util.Map;

public class EndpointsHandler {

  // TODO handle dynamic paths
  private final Map<String, Map<String, EndpointDefinition>> endpointDefinitions = new HashMap<>();

  public void define(Garcon garcon, @DelegatesTo(EndpointDefiner.class) Closure<?> closure)  {
    // using setter to avoid having callsite on compiled code
    closure.setDelegate(new EndpointDefiner(garcon, endpointDefinitions));
    closure.setResolveStrategy(Closure.DELEGATE_FIRST);
    closure.call();
  }

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
