package com.tambapps.http.garcon

class EndpointDefiner {

  private List<EndpointDefinition> endpointDefinitions = []

  void get(String path, @DelegatesTo(Garcon.Context) Closure closure) {
    endpointDefinitions.add(new EndpointDefinition(method: 'GET', path: path, closure: closure))
  }

  void delete(String path, @DelegatesTo(Garcon.Context) Closure closure) {
    closure.resolveStrategy = Closure.DELEGATE_FIRST
    endpointDefinitions.add(new EndpointDefinition(method: 'DELETE', path: path, closure: closure))
  }

  List<EndpointDefinition> getEndpointDefinitions() {
    return endpointDefinitions
  }
}
