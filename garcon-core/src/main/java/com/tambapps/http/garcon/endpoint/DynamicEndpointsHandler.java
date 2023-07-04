package com.tambapps.http.garcon.endpoint;

import com.tambapps.http.garcon.exception.MethodNotAllowedException;
import com.tambapps.http.garcon.exception.NotFoundException;
import lombok.Value;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Endpoints handler that supports both static and dynamic paths, at the cost of taking more time
 * to find a specific endpoint
 */
public class DynamicEndpointsHandler<T> extends StaticEndpointsHandler<T> {

  private static final Pattern PATH_VARIABLE_PATTERN = Pattern.compile("\\{(\\w+)\\}");
  final List<DynamicEndpointsDefinition<T>> dynamicPaths = new ArrayList<>();

  @Override
  public EndpointDefinition<T> getEndpoint(String path, String method)
      throws NotFoundException, MethodNotAllowedException {
    for (DynamicEndpointsDefinition<T> dynamicPath : dynamicPaths) {
      if (dynamicPath.getPattern().matcher(path).matches()) {
        // found path matching this definition
        EndpointDefinition<T> endpointDefinition = dynamicPath.getMethodEndpointMap().get(method);
        if (endpointDefinition == null) {
          throw new MethodNotAllowedException();
        }
        return endpointDefinition;
      }
    }
    return super.getEndpoint(path, method);
  }

  boolean isStatic() {
    return dynamicPaths.isEmpty();
  }

  StaticEndpointsHandler<T> asStatic() {
    return new StaticEndpointsHandler<>(endpointDefinitions);
  }

  void mergeWith(EndpointsHandler<T> handler) {
    if (handler instanceof DynamicEndpointsHandler) {
      endpointDefinitions.putAll(((DynamicEndpointsHandler<T>) handler).endpointDefinitions);
      dynamicPaths.addAll(((DynamicEndpointsHandler<T>) handler).dynamicPaths);
    } else if (handler instanceof StaticEndpointsHandler) {
      endpointDefinitions.putAll(((StaticEndpointsHandler<T>) handler).endpointDefinitions);
    } else {
      throw new IllegalArgumentException(
          String.format("Unknown subclass %s of EndpointsHandler", handler.getClass()));
    }
  }

  @Override
  public boolean isEmpty() {
    return dynamicPaths.isEmpty() && super.isEmpty();
  }

  @Override
  public void defineEndpoint(String path, String method, EndpointDefinition<T> endpointDefinition) {
    Matcher matcher = PATH_VARIABLE_PATTERN.matcher(path);
    List<String> pathVariableNames = new ArrayList<>();
    while (matcher.find()) {
      pathVariableNames.add(matcher.group(1));
    }
    if (!pathVariableNames.isEmpty()) {
      defineDynamicEndpoint(path, method, pathVariableNames, endpointDefinition);
    } else {
      super.defineEndpoint(path, method, endpointDefinition);
    }
  }

  public void defineDynamicEndpoint(String path, String method, List<String> pathVariableNames,
      EndpointDefinition<T> endpointDefinition) {
    Matcher m = PATH_VARIABLE_PATTERN.matcher(path);
    StringBuffer sb = new StringBuffer();
    while (m.find()) {
      // yes, we neeed to backslash the backslash because the replacement string is not interpreted as a litteral string
      m.appendReplacement(sb, "(\\\\w+)");
    }
    m.appendTail(sb);
    // in case of trailing slash
    sb.append("/?");
    Pattern pattern = Pattern.compile(sb.toString());
    Optional<DynamicEndpointsDefinition<T>> optDefinition = dynamicPaths.stream()
        .filter(d -> d.getPattern().pattern().equals(pattern.pattern()))
        .findFirst();

    DynamicEndpointsDefinition<T> definition;
    if (optDefinition.isPresent()) {
      definition = optDefinition.get();
    } else {
      definition = new DynamicEndpointsDefinition<>(Pattern.compile(sb.toString()), new HashMap<>());
      dynamicPaths.add(definition);
    }

    if (definition.getMethodEndpointMap().containsKey(method)) {
      throw new IllegalStateException(
          String.format("Endpoint %s %s is already defined", method, path));
    }
    definition.getMethodEndpointMap()
        .put(method, endpointDefinition.toDynamic(pathVariableNames, pattern));
  }

  @Value
  private static class DynamicEndpointsDefinition<T> {
    Pattern pattern;

    Map<String, EndpointDefinition<T>> methodEndpointMap;
  }
}
