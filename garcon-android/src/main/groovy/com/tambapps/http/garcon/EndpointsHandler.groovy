package com.tambapps.http.garcon

import java.nio.file.Paths

class EndpointsHandler {

  private final List<EndpointDefinition> endpointDefinitions = []

  void define(Garcon garcon, @DelegatesTo(EndpointDefiner) Closure closure) {
    closure.delegate = new EndpointDefiner(garcon, endpointDefinitions)
    closure.resolveStrategy = Closure.DELEGATE_FIRST
    closure.call()
    if (endpointDefinitions.unique { it.method + it.path }.size() != endpointDefinitions.size()) {
      throw new IllegalStateException('There are some duplicate endpoints')
    }
  }

  EndpointDefinition getMatchingEndpointDefinition(String p) {
    def path = Paths.get(p)
    return endpointDefinitions.find { it.matches(path) }
  }

}
