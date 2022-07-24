package com.tambapps.http.garcon

import groovy.transform.Immutable
import groovy.transform.PackageScope

@Immutable
@PackageScope
class EndpointDefinition {

  String method
  String path
  Closure closure
}
