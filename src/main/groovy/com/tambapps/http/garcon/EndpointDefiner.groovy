package com.tambapps.http.garcon

class EndpointDefiner {

  EndpointDefiner(List<EndpointDefinition> endpointDefinitions) {
    this.endpointDefinitions = endpointDefinitions
  }
  private List<EndpointDefinition> endpointDefinitions

  void get(String path, @DelegatesTo(Garcon.Context) Closure closure) {
    endpointDefinitions.add(new EndpointDefinition(method: 'GET', path: path, closure: closure))
  }

  void delete(String path, @DelegatesTo(Garcon.Context) Closure closure) {
    endpointDefinitions.add(new EndpointDefinition(method: 'DELETE', path: path, closure: closure))
  }

}
