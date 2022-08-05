package com.tambapps.http.garcon

import groovy.transform.PackageScope

import java.nio.file.Path
import java.nio.file.Paths

@PackageScope
class EndpointDefinition {

  String method
  String path
  private Closure closure
  ContentType accept
  ContentType contentType

  void rehydrate(Object delegate) {
    closure.delegate = delegate
    closure.resolveStrategy = Closure.DELEGATE_FIRST
  }

  Object call() {
    return closure.call()
  }

  boolean matches(Path otherPath) {
    return Paths.get(path.startsWith('/') ? path : ("/$path".toString())) == otherPath
  }
}
