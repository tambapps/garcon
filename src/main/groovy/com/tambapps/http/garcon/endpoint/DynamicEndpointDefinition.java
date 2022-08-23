package com.tambapps.http.garcon.endpoint;

import com.tambapps.http.garcon.ContentType;
import com.tambapps.http.garcon.HttpExchangeContext;
import com.tambapps.http.garcon.HttpResponse;
import groovy.lang.Closure;
import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
public class DynamicEndpointDefinition extends EndpointDefinition {

  private final List<String> pathVariableNames;
  private final Pattern pathVariablePattern;

  public DynamicEndpointDefinition(ThreadLocal<OptimizedClosure> threadLocalClosure,
      ContentType accept, ContentType contentType, List<String> pathVariableNames,
      Pattern pathVariablePattern) {
    super(threadLocalClosure, accept, contentType);
    this.pathVariableNames = pathVariableNames;
    this.pathVariablePattern = pathVariablePattern;
  }

  @Override
  public HttpResponse call(HttpExchangeContext context) {
    Map<String, String> pathVariables = new HashMap<>();
    Matcher matcher = pathVariablePattern.matcher(context.getPath());
    while (matcher.find());
    // TODO test me
    for (int i = 0; i < pathVariableNames.size(); i++) {
      pathVariables.put(pathVariableNames.get(i), matcher.group(i));
    }
    context.setPathVariables(pathVariables);
    return super.call(context);
  }
}
