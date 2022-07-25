package com.tambapps.http.garcon

import groovy.transform.PackageScope

@PackageScope
class EndpointDefinition {

  String method
  String path
  private Closure closure
  // TODO use me for request body
  ContentType accept
  ContentType contentType

  void rehydrate(Object delegate) {
    closure.delegate = delegate
    closure.resolveStrategy = Closure.DELEGATE_FIRST
  }

  Object call() {
    return closure.call()
  }
}
