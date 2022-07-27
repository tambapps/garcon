package com.tambapps.http.garcon

import groovy.transform.NamedParam
import groovy.transform.PackageScope

class EndpointDefiner {

  private final Garcon garcon
  private final List<EndpointDefinition> endpointDefinitions

  @PackageScope
  EndpointDefiner(Garcon garcon, List<EndpointDefinition> endpointDefinitions) {
    this.garcon = garcon
    this.endpointDefinitions = endpointDefinitions
  }

  void get(String path, @DelegatesTo(Garcon.Context) Closure closure) {
    get(Collections.emptyMap(), path, closure)
  }

  void get(
      @NamedParam(value = 'accept', type = ContentType.class)
      @NamedParam(value = 'contentType', type = ContentType.class)
      Map<?, ?> additionalParameters, String path, @DelegatesTo(Garcon.Context) Closure closure) {
    endpointDefinitions.add(new EndpointDefinition(method: 'GET', path: path, closure: closure,
        accept: additionalParameters.accept, contentType: additionalParameters.contentType))
  }

  void delete(String path, @DelegatesTo(Garcon.Context) Closure closure) {
    delete(Collections.emptyMap(), path, closure)
  }

  void delete(
      @NamedParam(value = 'contentType', type = ContentType.class)
      Map<?, ?> additionalParameters, String path, @DelegatesTo(Garcon.Context) Closure closure) {
    endpointDefinitions.add(new EndpointDefinition(method: 'DELETE', path: path, closure: closure,
        accept: additionalParameters.accept, contentType: additionalParameters.contentType))
  }

  void setContentType(ContentType contentType) {
    garcon.contentType =contentType
  }

  void setAccept(ContentType contentType) {
    garcon.accept = contentType
  }
}
