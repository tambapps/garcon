package com.tambapps.http.garcon

import groovy.transform.PackageScope

@PackageScope
class HttpExchangeHandlerRunnable extends AbstractHttpExchangeHandler {

  private final AndroidGarcon garcon
  private final EndpointsHandler endpointsHandler

  HttpExchangeHandlerRunnable(Socket socket, AndroidGarcon garcon, EndpointsHandler endpointsHandler, Collection<Closeable> connections) {
    super(socket, garcon, connections)
    this.garcon = garcon
    this.endpointsHandler = endpointsHandler
  }

  @Override
  List<EndpointDefinition> findPathEndpoints(String path) {
    return endpointsHandler.getDefinitionsForPath(path)
  }

  @PackageScope
  @Override
  void onConnectionClosed(IOException e) {
    garcon.onConnectionClosed?.call(e)
  }

  @PackageScope
  @Override
  void onConnectionError(IOException e) {
    garcon.onConnectionError?.call(e)
  }

  @PackageScope
  @Override
  void onUnexpectedError(Exception e) {
    garcon.onConnectionUnexpectedError?.call(e)
  }
}
