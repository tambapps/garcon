package com.tambapps.http.garcon

import java.nio.file.Paths

class EndpointsHandler {

  private final List<EndpointDefinition> endpointDefinitions = []
  private final Garcon.Context context

  EndpointsHandler(Garcon.Context context) {
    this.context = context
  }

  void define(@DelegatesTo(EndpointDefiner) Closure closure) {
    closure.delegate = new EndpointDefiner(endpointDefinitions: endpointDefinitions)
    closure.resolveStrategy = Closure.DELEGATE_FIRST
    closure.call()
  }

  EndpointDefinition getAndRehydrateMatchingEndpointDefinition(String p) {
    def path = Paths.get(p)
    def definition = endpointDefinitions.find { Paths.get(it.path) == path }
    definition?.rehydrate(context)
    return definition
  }

}
