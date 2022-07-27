package com.tambapps.http.garcon

import java.nio.file.Paths

class EndpointsHandler {

  private final List<EndpointDefinition> endpointDefinitions = []

  void define(Garcon garcon, @DelegatesTo(EndpointDefiner) Closure closure) {
    closure.delegate = new EndpointDefiner(garcon, endpointDefinitions)
    closure.resolveStrategy = Closure.DELEGATE_FIRST
    closure.call()
  }

  EndpointDefinition getMatchingEndpointDefinition(String p) {
    def path = Paths.get(p)
    return endpointDefinitions.find { Paths.get(it.path) == path }
  }

}
