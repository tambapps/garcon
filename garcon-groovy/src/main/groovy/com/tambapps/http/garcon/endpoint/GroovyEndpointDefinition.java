package com.tambapps.http.garcon.endpoint;

import com.tambapps.http.garcon.ContentType;
import com.tambapps.http.garcon.HttpExchangeContext;
import groovy.lang.Closure;

import java.util.List;
import java.util.regex.Pattern;

public class GroovyEndpointDefinition extends EndpointDefinition<Closure<?>> {

  // needed because we modify the delegate before calling the closure
  private final ThreadLocal<Closure<?>> threadLocalClosure;

  /**
   * Constructs an endpoint definition
   *
   * @param accept             the accept content type (request)
   * @param contentType        the response content type
   */
  public GroovyEndpointDefinition(Closure<?> closure, ContentType accept, ContentType contentType) {
    this(ThreadLocal.withInitial(() -> (Closure<?>) closure.clone()), accept, contentType);
  }

  protected GroovyEndpointDefinition(ThreadLocal<Closure<?>> threadLocalClosure, ContentType accept, ContentType contentType) {
    super(accept, contentType);
    this.threadLocalClosure = threadLocalClosure;
  }

  @Override
  Object doCall(HttpExchangeContext context) {
    // rehydrating
    Closure<?> closure = threadLocalClosure.get();

    closure.setDelegate(context);
    return closure.call(context);
  }

  @Override
  public DynamicEndpointDefinition<Closure<?>> toDynamic(List<String> pathVariableNames, Pattern pattern) {
    return new GroovyDynamicEndpointDefinition(threadLocalClosure, getAccept(), getContentType(), pathVariableNames, pattern);
  }
}
