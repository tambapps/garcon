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
public class DynamicEndpointsHandler extends StaticEndpointsHandler {

  private static final Pattern PATH_VARIABLE_PATTERN = Pattern.compile("\\{(\\w+)\\}");
  final List<DynamicEndpointsDefinition> dynamicPaths = new ArrayList<>();

  @Override
  public EndpointDefinition getEndpoint(String path, String method)
      throws NotFoundException, MethodNotAllowedException {
    for (DynamicEndpointsDefinition dynamicPath : dynamicPaths) {
      if (dynamicPath.getPattern().matcher(path).matches()) {
        // found path matching this definition
        EndpointDefinition endpointDefinition = dynamicPath.getMethodEndpointMap().get(method);
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

  StaticEndpointsHandler asStatic() {
    return new StaticEndpointsHandler(endpointDefinitions);
  }

  void mergeWith(EndpointsHandler handler) {
    if (handler instanceof DynamicEndpointsHandler) {
      endpointDefinitions.putAll(((DynamicEndpointsHandler) handler).endpointDefinitions);
      dynamicPaths.addAll(((DynamicEndpointsHandler) handler).dynamicPaths);
    } else if (handler instanceof StaticEndpointsHandler) {
      endpointDefinitions.putAll(((StaticEndpointsHandler) handler).endpointDefinitions);
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
  public void defineEndpoint(String path, String method, EndpointDefinition endpointDefinition) {
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
      EndpointDefinition endpointDefinition) {
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
    Optional<DynamicEndpointsDefinition> optDefinition = dynamicPaths.stream()
        .filter(d -> d.getPattern().pattern().equals(pattern.pattern()))
        .findFirst();

    DynamicEndpointsDefinition definition;
    if (optDefinition.isPresent()) {
      definition = optDefinition.get();
    } else {
      definition = new DynamicEndpointsDefinition(Pattern.compile(sb.toString()), new HashMap<>());
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
  private static class DynamicEndpointsDefinition {
    Pattern pattern;

    Map<String, EndpointDefinition> methodEndpointMap;
  }
}
