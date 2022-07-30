package com.tambapps.http.garcon

import com.tambapps.http.garcon.io.composer.Composers
import com.tambapps.http.garcon.io.composer.Parsers
import com.tambapps.http.garcon.util.ContentTypeMap
import groovy.transform.PackageScope
import org.codehaus.groovy.runtime.DefaultGroovyMethods

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class Garcon extends AbstractGarcon {

  @PackageScope
  final EndpointsHandler endpointsHandler = new EndpointsHandler()
  final ContentTypeMap<Closure<?>> composers = Composers.map
  final ContentTypeMap<Closure<?>> parsers = Parsers.map

  ContentType accept
  ContentType contentType


  void define(@DelegatesTo(EndpointsHandler) Closure closure) {
    endpointsHandler.define(this, closure)
  }

  void serve(@DelegatesTo(EndpointDefiner) Closure closure) {
    define(closure)
    start()
  }

  void serveAsync(@DelegatesTo(EndpointDefiner) Closure closure) {
    define(closure)
    startAsync()
  }

  @Override
  Runnable newExchangeHandler(Socket socket, AbstractGarcon garcon, Collection<Closeable> connections) {
    return new HttpExchangeHandler(socket, this, connections)
  }
}
