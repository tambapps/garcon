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
        accept: additionalParameters.accept, contentType: additionalParameters.contentType))
  }

  void post(String path, @DelegatesTo(HttpExchangeContext) Closure closure) {
    post(Collections.emptyMap(), path, closure)
  }

  void post(
      @NamedParam(value = 'accept', type = ContentType.class)
      @NamedParam(value = 'contentType', type = ContentType.class)
      Map<?, ?> additionalParameters, String path, @DelegatesTo(HttpExchangeContext) Closure closure) {
    endpointDefinitions.add(new EndpointDefinition(method: 'POST', path: path, closure: closure,
        accept: additionalParameters.accept, contentType: additionalParameters.contentType))
  }

  void patch(String path, @DelegatesTo(HttpExchangeContext) Closure closure) {
    patch(Collections.emptyMap(), path, closure)
  }

  void patch(
      @NamedParam(value = 'accept', type = ContentType.class)
      @NamedParam(value = 'contentType', type = ContentType.class)
      Map<?, ?> additionalParameters, String path, @DelegatesTo(HttpExchangeContext) Closure closure) {
    endpointDefinitions.add(new EndpointDefinition(method: 'PATCH', path: path, closure: closure,
        accept: additionalParameters.accept, contentType: additionalParameters.contentType))
  }

  void get(String path, @DelegatesTo(HttpExchangeContext) Closure closure) {
    get(Collections.emptyMap(), path, closure)
  }

  void get(
      @NamedParam(value = 'contentType', type = ContentType.class)
      Map<?, ?> additionalParameters, String path, @DelegatesTo(HttpExchangeContext) Closure closure) {
    endpointDefinitions.add(new EndpointDefinition(method: 'GET', path: path, closure: closure,
        contentType: additionalParameters.contentType))
  }

  void delete(String path, @DelegatesTo(HttpExchangeContext) Closure closure) {
    delete(Collections.emptyMap(), path, closure)
  }

  void delete(
      @NamedParam(value = 'contentType', type = ContentType.class)
      Map<?, ?> additionalParameters, String path, @DelegatesTo(HttpExchangeContext) Closure closure) {
    endpointDefinitions.add(new EndpointDefinition(method: 'DELETE', path: path, closure: closure,
        contentType: additionalParameters.contentType))
  }

  void setContentType(ContentType contentType) {
    garcon.contentType =contentType
  }

  void setAccept(ContentType contentType) {
    garcon.accept = contentType
  }

  Closure file(String path) {
    return file(Collections.emptyMap(), path)
  }

  Closure file(Path path) {
    return file(Collections.emptyMap(), path)
  }

  Closure file(@NamedParam(value = 'contentType', type = ContentType.class)
                   Map<?, ?> additionalParameters, String path) {
    return file(additionalParameters, new File(path))
  }

  Closure file(@NamedParam(value = 'contentType', type = ContentType.class)
                   Map<?, ?> additionalParameters, Path path) {
    return file(additionalParameters, path.toFile())
  }

  Closure file(File f) {
    return file(Collections.emptyMap(), f)
  }

  Closure file(
      @NamedParam(value = 'contentType', type = ContentType.class)
          Map<?, ?> additionalParameters,
      File f) {
    return {
      response.body = new FileInputStream(f)
      if (additionalParameters.contentType) {
        response.contentType = additionalParameters.contentType
      }
    }
  }

}
