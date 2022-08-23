package com.tambapps.http.garcon;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.transform.NamedParam;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class EndpointDefiner {

  private final Garcon garcon;
  // path -> method -> endpoint
  private final Map<String, Map<String, EndpointDefinition>> endpointDefinitions;

  EndpointDefiner(Garcon garcon, Map<String, Map<String, EndpointDefinition>> endpointDefinitions) {
    this.garcon = garcon;
    this.endpointDefinitions = endpointDefinitions;
  }

  public void put(String path, @DelegatesTo(HttpExchangeContext.class) Closure<?> closure) {
    put(Collections.emptyMap(), path, closure);
  }

  public void put(
      @NamedParam(value = "accept", type = ContentType.class)
      @NamedParam(value = "contentType", type = ContentType.class)
      Map<?, ?> additionalParameters, String path, @DelegatesTo(HttpExchangeContext.class) Closure<?> closure) {
    method(additionalParameters, "PUT", path, closure);
  }

  public void post(String path, @DelegatesTo(HttpExchangeContext.class) Closure<?> closure) {
    post(Collections.emptyMap(), path, closure);
  }

  public void post(
      @NamedParam(value = "accept", type = ContentType.class)
      @NamedParam(value = "contentType", type = ContentType.class)
      Map<?, ?> additionalParameters, String path, @DelegatesTo(HttpExchangeContext.class) Closure<?> closure) {
    method(additionalParameters, "POST", path, closure);
  }

  public void patch(String path, @DelegatesTo(HttpExchangeContext.class) Closure<?> closure) {
    patch(Collections.emptyMap(), path, closure);
  }

  public void patch(
      @NamedParam(value = "accept", type = ContentType.class)
      @NamedParam(value = "contentType", type = ContentType.class)
      Map<?, ?> additionalParameters, String path, @DelegatesTo(HttpExchangeContext.class) Closure<?> closure) {
    method(additionalParameters, "PATCH", path, closure);
  }

  public void get(String path, @DelegatesTo(HttpExchangeContext.class) Closure<?> closure) {
    get(Collections.emptyMap(), path, closure);
  }

  public void get(
      @NamedParam(value = "contentType", type = ContentType.class)
      Map<?, ?> additionalParameters, String path, @DelegatesTo(HttpExchangeContext.class) Closure<?> closure) {
    method(additionalParameters, "GET", path, closure);
  }

  public void delete(String path, @DelegatesTo(HttpExchangeContext.class) Closure<?> closure) {
    delete(Collections.emptyMap(), path, closure);
  }

  public void delete(
      @NamedParam(value = "contentType", type = ContentType.class)
      Map<?, ?> additionalParameters, String path, @DelegatesTo(HttpExchangeContext.class) Closure<?> closure) {
    method(additionalParameters, "DELETE", path, closure);
  }


  public void method(@NamedParam(value = "accept", type = ContentType.class)
      @NamedParam(value = "contentType", type = ContentType.class)
          Map<?, ?> additionalParameters, String method, String path, @DelegatesTo(HttpExchangeContext.class) Closure<?> closure) {
    addEndpoint(path, method, new EndpointDefinition(closure, (ContentType) additionalParameters.get("accept"),
        (ContentType) additionalParameters.get("contentType")));
  }

  public void setContentType(ContentType contentType) {
    garcon.setContentType(contentType);
  }

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

  private static class FileClosure extends Closure<Object> {

    private final File f;
    public FileClosure(File f) {
      super(null);
      this.f = f;
    }

    public void doCall() throws IOException  {
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
    Map<String, EndpointDefinition> methodMap =
        endpointDefinitions.computeIfAbsent(path, k -> new HashMap<>());
    if (methodMap.containsKey(method)) {
      throw new IllegalStateException(String.format("Endpoint %s %s is already defined", method, path));
    }
    methodMap.put(method.toUpperCase(), endpointDefinition);
  }
}
