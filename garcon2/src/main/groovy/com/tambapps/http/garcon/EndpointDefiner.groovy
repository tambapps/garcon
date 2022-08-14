package com.tambapps.http.garcon

import groovy.transform.NamedParam
import groovy.transform.PackageScope

import java.nio.file.Path

class EndpointDefiner {

  private final Garcon garcon
  // path -> method -> endpoint
  private final Map<String, Map<String, EndpointDefinition>> endpointDefinitions

  @PackageScope
  EndpointDefiner(Garcon garcon, Map<String, Map<String, EndpointDefinition>> endpointDefinitions) {
    this.garcon = garcon
    this.endpointDefinitions = endpointDefinitions
  }

  void put(String path, @DelegatesTo(HttpExchangeContext) Closure closure) {
    put(Collections.emptyMap(), path, closure)
  }

  void put(
      @NamedParam(value = 'accept', type = ContentType.class)
      @NamedParam(value = 'contentType', type = ContentType.class)
      Map<?, ?> additionalParameters, String path, @DelegatesTo(HttpExchangeContext) Closure closure) {
    method(additionalParameters, 'PUT', path, closure)
  }

  void post(String path, @DelegatesTo(HttpExchangeContext) Closure closure) {
    post(Collections.emptyMap(), path, closure)
  }

  void post(
      @NamedParam(value = 'accept', type = ContentType.class)
      @NamedParam(value = 'contentType', type = ContentType.class)
      Map<?, ?> additionalParameters, String path, @DelegatesTo(HttpExchangeContext) Closure closure) {
    method(additionalParameters, 'POST', path, closure)
  }

  void patch(String path, @DelegatesTo(HttpExchangeContext) Closure closure) {
    patch(Collections.emptyMap(), path, closure)
  }

  void patch(
      @NamedParam(value = 'accept', type = ContentType.class)
      @NamedParam(value = 'contentType', type = ContentType.class)
      Map<?, ?> additionalParameters, String path, @DelegatesTo(HttpExchangeContext) Closure closure) {
    method(additionalParameters, 'PATCH', path, closure)
  }

  void get(String path, @DelegatesTo(HttpExchangeContext) Closure closure) {
    get(Collections.emptyMap(), path, closure)
  }

  void get(
      @NamedParam(value = 'contentType', type = ContentType.class)
      Map<?, ?> additionalParameters, String path, @DelegatesTo(HttpExchangeContext) Closure closure) {
    method(additionalParameters, 'GET', path, closure)
  }

  void delete(String path, @DelegatesTo(HttpExchangeContext) Closure closure) {
    delete(Collections.emptyMap(), path, closure)
  }

  void delete(
      @NamedParam(value = 'contentType', type = ContentType.class)
      Map<?, ?> additionalParameters, String path, @DelegatesTo(HttpExchangeContext) Closure closure) {
    method(additionalParameters, 'DELETE', path, closure)
  }


  void method(@NamedParam(value = 'accept', type = ContentType.class)
      @NamedParam(value = 'contentType', type = ContentType.class)
          Map<?, ?> additionalParameters, String method, String path, @DelegatesTo(HttpExchangeContext) Closure closure) {
    addEndpoint(path, method, new EndpointDefinition(closure: closure, contentType: (ContentType) additionalParameters.contentType,
        accept: (ContentType) additionalParameters.accept))
  }

  void setContentType(ContentType contentType) {
    garcon.contentType =contentType
  }

  void setAccept(ContentType contentType) {
    garcon.accept = contentType
  }

  void file(String path) {
    file(Collections.emptyMap(), path)
  }

  void file(Path path) {
    file(Collections.emptyMap(), path)
  }

  void file(@NamedParam(value = 'contentType', type = ContentType.class)
                   Map<?, ?> additionalParameters, String path) {
    file(additionalParameters, new File(path))
  }

  void file(@NamedParam(value = 'contentType', type = ContentType.class)
                   Map<?, ?> additionalParameters, Path path) {
    file(additionalParameters, path.toFile())
  }

  void file(File f) {
    file(Collections.emptyMap(), f)
  }

  void file(
      @NamedParam(value = 'contentType', type = ContentType.class)
          Map<?, ?> additionalParameters,
      File f) {
    // TODO make http path configurable
    get(f.name, contentType: (ContentType) additionalParameters.contentType) {
      HttpResponse response = (HttpResponse) getProperty('response')
      response.body = new FileInputStream(f)
    }
  }

  private void addEndpoint(String path, String method, EndpointDefinition endpointDefinition) {
    if (path.endsWith('/')) {
      path = path.substring(0, path.length() - 1)
    }
    if (!path.startsWith('/')) {
      path = "/$path"
    }
    def methodMap = endpointDefinitions.get(path)
    if (methodMap == null) {
      methodMap = new HashMap<String, EndpointDefinition>()
      endpointDefinitions.put(path, methodMap)
    }
    if (methodMap.containsKey(method)) {
      throw new IllegalStateException("Endpoint$method $path is already defined")
    }
    methodMap.put(method.toUpperCase(), endpointDefinition)
  }
}
