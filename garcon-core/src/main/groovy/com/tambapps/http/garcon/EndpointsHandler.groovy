package com.tambapps.http.garcon

import java.nio.file.Paths

class EndpointsHandler {

  private final List<EndpointDefinition> endpointDefinitions = []

  void define(AbstractGarcon garcon, @DelegatesTo(EndpointDefiner) Closure closure) {
    // using setter to avoid having callsite on compiled code
    closure.setDelegate(new EndpointDefiner(garcon, endpointDefinitions))
    closure.setResolveStrategy(Closure.DELEGATE_FIRST)
    closure.call()
    if (endpointDefinitions.unique { it.method + it.path }.size() != endpointDefinitions.size()) {
      throw new IllegalStateException('There are some duplicate endpoints')
    }
  }

  EndpointDefinition getMatchingEndpointDefinition(String p) {
    def path = Paths.get(p)
    return endpointDefinitions.find { it.matches(path) }
  }

  Map<String, List<EndpointDefinition>> getDefinitionsPerPath() {
    return endpointDefinitions.groupBy { it.path }
  }
}
