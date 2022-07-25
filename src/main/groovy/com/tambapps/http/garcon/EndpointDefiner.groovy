package com.tambapps.http.garcon

import groovy.transform.NamedParam

class EndpointDefiner {

  EndpointDefiner(List<EndpointDefinition> endpointDefinitions) {
    this.endpointDefinitions = endpointDefinitions
  }
  private List<EndpointDefinition> endpointDefinitions

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

}
