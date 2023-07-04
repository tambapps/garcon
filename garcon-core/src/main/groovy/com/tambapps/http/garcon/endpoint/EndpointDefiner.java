package com.tambapps.http.garcon.endpoint;

import com.tambapps.http.garcon.ContentType;
import com.tambapps.http.garcon.AbstractGarcon;
import groovy.lang.Closure;

/**
 * Delegate of {@link AbstractGarcon#define(Closure)} closure, allowing to define endpoints in a Groovy way
 */
public abstract class EndpointDefiner<T> {

  private final AbstractGarcon garcon;

  // path -> method -> endpoint
  private final DynamicEndpointsHandler endpointsHandler;

  // handler is nullable
  EndpointDefiner(AbstractGarcon garcon, EndpointsHandler handler) {
    this.garcon = garcon;
    DynamicEndpointsHandler endpointsHandler = new DynamicEndpointsHandler();
    if (handler != null) {
      endpointsHandler.mergeWith(handler);
    }
    this.endpointsHandler = endpointsHandler;
  }

  public abstract void method(String method, String path,
                     Object accept, Object contentType, T closure);

  /**
   * Sets the underlying garcon's response content type
   *
   * @param contentType the content type
   */
  public void setContentType(ContentType contentType) {
    garcon.setContentType(contentType);
  }

  /**
   * Sets the underlying garcon's accept content type
   *
   * @param contentType the content type
   */
  public void setAccept(ContentType contentType) {
    garcon.setAccept(contentType);
  }

  /**
   * Build an endpoint handler
   * @return the built endpoint handler
   */
  public EndpointsHandler<T> build() {
    return endpointsHandler.isStatic() ? endpointsHandler.asStatic() : endpointsHandler;
  }

  protected static ContentType toGarconContentType(Object o) {
    if (o == null) {
      return null;
    } else if (o instanceof ContentType) {
      return (ContentType) o;
    } else if (o.getClass().getName().equals("com.tambapps.http.hyperpoet.ContentType")) {
      return ContentType.fromHyperPoetContentType(o);
    } else {
      throw new ClassCastException(String.format("Could not cast class %s as ContentType", o.getClass()));
    }
  }

  protected void addEndpoint(String path, String method, EndpointDefinition endpointDefinition) {
    if (path.endsWith("/")) {
      path = path.substring(0, path.length() - 1);
    }
    if (!path.startsWith("/")) {
      path = "/" + path;
    }
    endpointsHandler.defineEndpoint(path, method, endpointDefinition);
  }

}
