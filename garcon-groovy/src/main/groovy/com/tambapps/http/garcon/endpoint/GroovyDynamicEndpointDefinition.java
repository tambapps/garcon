package com.tambapps.http.garcon.endpoint;

import com.tambapps.http.garcon.ContentType;
import com.tambapps.http.garcon.HttpExchangeContext;
import groovy.lang.Closure;

import java.util.List;
import java.util.regex.Pattern;

public class GroovyDynamicEndpointDefinition extends DynamicEndpointDefinition<Closure<?>> {

  // needed because we modify the delegate before calling the closure
  private final ThreadLocal<Closure<?>> threadLocalClosure;

  protected GroovyDynamicEndpointDefinition(ThreadLocal<Closure<?>> threadLocalClosure, ContentType accept, ContentType contentType, List<String> pathVariableNames,
                                            Pattern pathVariablePattern) {
    super(accept, contentType, pathVariableNames, pathVariablePattern);
    this.threadLocalClosure = threadLocalClosure;
  }

  @Override
  Object doCall(HttpExchangeContext context) {
    // rehydrating
    Closure<?> closure = threadLocalClosure.get();

    closure.setDelegate(context);
    return closure.call(context);
  }

}
