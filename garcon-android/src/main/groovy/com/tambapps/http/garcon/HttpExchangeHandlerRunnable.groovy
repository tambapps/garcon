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
  protected HttpResponse processExchange(HttpRequest request) {
    EndpointDefinition endpointDefinition = endpointsHandler.getMatchingEndpointDefinition(request.path)
    if (endpointDefinition == null) {
      return default404Response()
    } else if (endpointDefinition.method != request.method) {
      return default405Response()
    }

    try {
      return endpointDefinition.call(new HttpExchangeContext(request, new HttpResponse(), garcon.composers, garcon.parsers,
              endpointDefinition.contentType ?: garcon.contentType,
              endpointDefinition.accept ?: garcon.accept))
    } catch (Exception e) {
      onUnexpectedError(e)
      return default500Response()
    }
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
