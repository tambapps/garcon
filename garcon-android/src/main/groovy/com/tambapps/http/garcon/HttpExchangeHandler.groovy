package com.tambapps.http.garcon

import groovy.transform.PackageScope

@PackageScope
class HttpExchangeHandler extends AbstractHttpExchangeHandler {

  private final AndroidGarcon garcon
  private final EndpointsHandler endpointsHandler

  HttpExchangeHandler(Socket socket, AndroidGarcon garcon, EndpointsHandler endpointsHandler, Collection<Closeable> connections) {
    super(socket, garcon, connections)
    this.garcon = garcon
    this.endpointsHandler = endpointsHandler
  }

  @Override
  protected HttpResponse processExchange(HttpRequest request) {
    EndpointDefinition endpointDefinition = endpointsHandler.getMatchingEndpointDefinition(request.path)
    if (endpointDefinition == null) {
      return new HttpResponse().tap {
        statusCode = HttpStatus.NOT_FOUND
      }
    } else if (endpointDefinition.method != request.method) {
      return new HttpResponse().tap {
        statusCode = HttpStatus.METHOD_NOT_ALLOWED
      }
    }

    try {
      return endpointDefinition.call(new HttpExchangeContext(request, new HttpResponse(), garcon.composers, garcon.parsers,
              endpointDefinition.contentType ?: garcon.contentType,
              endpointDefinition.accept ?: garcon.accept))
    } catch (Exception e) {
      onUnexpectedError(e)
      return new HttpResponse().tap {
        statusCode = HttpStatus.INTERNAL_SERVER_ERROR
      }
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
