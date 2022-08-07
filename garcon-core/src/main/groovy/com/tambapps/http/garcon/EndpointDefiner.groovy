package com.tambapps.http.garcon

import groovy.transform.NamedParam
import groovy.transform.PackageScope

import java.nio.file.Path

class EndpointDefiner {

  private final Garcon garcon
  private final List<EndpointDefinition> endpointDefinitions

  @PackageScope
  EndpointDefiner(Garcon garcon, List<EndpointDefinition> endpointDefinitions) {
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
    endpointDefinitions.add(new EndpointDefinition(method: 'PUT', path: path, closure: closure,
        accept: (ContentType) additionalParameters.accept, contentType: (ContentType) additionalParameters.contentType))
  }

  void post(String path, @DelegatesTo(HttpExchangeContext) Closure closure) {
    post(Collections.emptyMap(), path, closure)
  }

  void post(
      @NamedParam(value = 'accept', type = ContentType.class)
      @NamedParam(value = 'contentType', type = ContentType.class)
      Map<?, ?> additionalParameters, String path, @DelegatesTo(HttpExchangeContext) Closure closure) {
    endpointDefinitions.add(new EndpointDefinition(method: 'POST', path: path, closure: closure,
        accept: (ContentType) additionalParameters.accept, contentType: (ContentType) additionalParameters.contentType))
  }

  void patch(String path, @DelegatesTo(HttpExchangeContext) Closure closure) {
    patch(Collections.emptyMap(), path, closure)
  }

  void patch(
      @NamedParam(value = 'accept', type = ContentType.class)
      @NamedParam(value = 'contentType', type = ContentType.class)
      Map<?, ?> additionalParameters, String path, @DelegatesTo(HttpExchangeContext) Closure closure) {
    endpointDefinitions.add(new EndpointDefinition(method: 'PATCH', path: path, closure: closure,
        accept: (ContentType) additionalParameters.accept, contentType: (ContentType) additionalParameters.contentType))
  }

  void get(String path, @DelegatesTo(HttpExchangeContext) Closure closure) {
    get(Collections.emptyMap(), path, closure)
  }

  void get(
      @NamedParam(value = 'contentType', type = ContentType.class)
      Map<?, ?> additionalParameters, String path, @DelegatesTo(HttpExchangeContext) Closure closure) {
    endpointDefinitions.add(new EndpointDefinition(method: 'GET', path: path, closure: closure,
        contentType: (ContentType) additionalParameters.contentType))
  }

  void delete(String path, @DelegatesTo(HttpExchangeContext) Closure closure) {
    delete(Collections.emptyMap(), path, closure)
  }

  void delete(
      @NamedParam(value = 'contentType', type = ContentType.class)
      Map<?, ?> additionalParameters, String path, @DelegatesTo(HttpExchangeContext) Closure closure) {
    endpointDefinitions.add(new EndpointDefinition(method: 'DELETE', path: path, closure: closure,
        contentType: (ContentType) additionalParameters.contentType))
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
}
