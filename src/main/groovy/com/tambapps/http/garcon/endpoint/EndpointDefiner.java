package com.tambapps.http.garcon.endpoint;

import com.tambapps.http.garcon.ContentType;
import com.tambapps.http.garcon.Garcon;
import com.tambapps.http.garcon.HttpExchangeContext;
import com.tambapps.http.garcon.HttpResponse;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.transform.NamedParam;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

/**
 * Delegate of {@link Garcon#define(Closure)} closure, allowing to define endpoints in a Groovy way
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class EndpointDefiner {

  private final Garcon garcon;

  // path -> method -> endpoint
  private final DynamicEndpointsHandler endpointsHandler;

  // handler is nullable
  public static EndpointDefiner newInstance(Garcon garcon, EndpointsHandler handler) {
    DynamicEndpointsHandler endpointsHandler = new DynamicEndpointsHandler();
    if (handler != null) {
      endpointsHandler.mergeWith(handler);
    }
    return new EndpointDefiner(garcon, endpointsHandler);
  }

  /**
   * Define a PUT endpoint
   *
   * @param path    the path of the endpoint
   * @param closure the closure, the behavior of the endpoint
   */
  public void put(String path, @DelegatesTo(HttpExchangeContext.class) Closure<?> closure) {
    put(Collections.emptyMap(), path, closure);
  }

  /**
   * Define a PUT endpoint
   *
   * @param additionalParameters the additional parameters if any
   * @param path                 the path of the endpoint
   * @param closure              the closure, the behavior of the endpoint
   */
  public void put(
      @NamedParam(value = "accept", type = ContentType.class)
      @NamedParam(value = "contentType", type = ContentType.class)
      Map<?, ?> additionalParameters, String path,
      @DelegatesTo(HttpExchangeContext.class) Closure<?> closure) {
    method(additionalParameters, "PUT", path, closure);
  }

  /**
   * Define a POST endpoint
   *
   * @param path    the path of the endpoint
   * @param closure the closure, the behavior of the endpoint
   */
  public void post(String path, @DelegatesTo(HttpExchangeContext.class) Closure<?> closure) {
    post(Collections.emptyMap(), path, closure);
  }

  /**
   * Define a POST endpoint
   *
   * @param additionalParameters the additional parameters if any
   * @param path                 the path of the endpoint
   * @param closure              the closure, the behavior of the endpoint
   */
  public void post(
      @NamedParam(value = "accept", type = ContentType.class)
      @NamedParam(value = "contentType", type = ContentType.class)
      Map<?, ?> additionalParameters, String path,
      @DelegatesTo(HttpExchangeContext.class) Closure<?> closure) {
    method(additionalParameters, "POST", path, closure);
  }

  /**
   * Define a PATCH endpoint
   *
   * @param path    the path of the endpoint
   * @param closure the closure, the behavior of the endpoint
   */
  public void patch(String path, @DelegatesTo(HttpExchangeContext.class) Closure<?> closure) {
    patch(Collections.emptyMap(), path, closure);
  }

  /**
   * Define a PATCH endpoint
   *
   * @param additionalParameters the additional parameters if any
   * @param path                 the path of the endpoint
   * @param closure              the closure, the behavior of the endpoint
   */
  public void patch(
      @NamedParam(value = "accept", type = ContentType.class)
      @NamedParam(value = "contentType", type = ContentType.class)
      Map<?, ?> additionalParameters, String path,
      @DelegatesTo(HttpExchangeContext.class) Closure<?> closure) {
    method(additionalParameters, "PATCH", path, closure);
  }

  /**
   * Define a GET endpoint
   *
   * @param path    the path of the endpoint
   * @param closure the closure, the behavior of the endpoint
   */
  public void get(String path, @DelegatesTo(HttpExchangeContext.class) Closure<?> closure) {
    get(Collections.emptyMap(), path, closure);
  }

  /**
   * Define a GET endpoint
   *
   * @param additionalParameters the additional parameters if any
   * @param path                 the path of the endpoint
   * @param closure              the closure, the behavior of the endpoint
   */
  public void get(
      @NamedParam(value = "contentType", type = ContentType.class)
      Map<?, ?> additionalParameters, String path,
      @DelegatesTo(HttpExchangeContext.class) Closure<?> closure) {
    method(additionalParameters, "GET", path, closure);
  }

  /**
   * Define a DELETE endpoint
   *
   * @param path    the path of the endpoint
   * @param closure the closure, the behavior of the endpoint
   */
  public void delete(String path, @DelegatesTo(HttpExchangeContext.class) Closure<?> closure) {
    delete(Collections.emptyMap(), path, closure);
  }

  /**
   * Define a DELETE endpoint
   *
   * @param additionalParameters the additional parameters if any
   * @param path                 the path of the endpoint
   * @param closure              the closure, the behavior of the endpoint
   */
  public void delete(
      @NamedParam(value = "contentType", type = ContentType.class)
      Map<?, ?> additionalParameters, String path,
      @DelegatesTo(HttpExchangeContext.class) Closure<?> closure) {
    method(additionalParameters, "DELETE", path, closure);
  }


  /**
   * Define an endpoint
   *
   * @param additionalParameters the additional parameters if any
   * @param method               the method
   * @param path                 the path of the endpoint
   * @param closure              the closure, the behavior of the endpoint
   */
  public void method(@NamedParam(value = "accept", type = ContentType.class)
  @NamedParam(value = "contentType", type = ContentType.class)
  Map<?, ?> additionalParameters, String method, String path,
      @DelegatesTo(HttpExchangeContext.class) Closure<?> closure) {
    addEndpoint(path, method,
        new EndpointDefinition(closure, getOptionalContentType(additionalParameters, "accept"),
            getOptionalContentType(additionalParameters, "contentType")));
  }

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

  public void file(String path) {
    file(Collections.emptyMap(), path);
  }

  public void file(Path path) {
    file(Collections.emptyMap(), path);
  }

  public void file(@NamedParam(value = "contentType", type = ContentType.class)
  Map<?, ?> additionalParameters, String path) {
    file(additionalParameters, new File(path));
  }

  public void file(@NamedParam(value = "contentType", type = ContentType.class)
  Map<?, ?> additionalParameters, Path path) {
    file(additionalParameters, path.toFile());
  }

  public void file(File f) {
    file(Collections.emptyMap(), f);
  }

  public void file(
      @NamedParam(value = "contentType", type = ContentType.class)
      Map<?, ?> additionalParameters,
      File f) {
    get(additionalParameters, f.getName(), new FileClosure(f));
  }

  /**
   * Build an endpoint handler
   * @return the built endpoint handler
   */
  public EndpointsHandler build() {
    return endpointsHandler.isStatic() ? endpointsHandler.asStatic() : endpointsHandler;
  }

  private static class FileClosure extends Closure<Object> {

    private final File f;

    public FileClosure(File f) {
      super(null);
      this.f = f;
    }

    public void doCall() throws IOException {
      HttpResponse response = (HttpResponse) getProperty("response");
      response.setBody(Files.newInputStream(f.toPath()));
    }
  }

  private void addEndpoint(String path, String method, EndpointDefinition endpointDefinition) {
    if (path.endsWith("/")) {
      path = path.substring(0, path.length() - 1);
    }
    if (!path.startsWith("/")) {
      path = "/" + path;
    }
    endpointsHandler.defineEndpoint(path, method, endpointDefinition);
  }

  private static ContentType getOptionalContentType(Map<?, ?> additionalParameters, String name) {
    Object o = additionalParameters.get(name);
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
}
