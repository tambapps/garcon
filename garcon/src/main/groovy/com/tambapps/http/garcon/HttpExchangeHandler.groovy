package com.tambapps.http.garcon

import groovy.transform.PackageScope

@PackageScope
class HttpExchangeHandler extends AbstractHttpExchangeHandler {

  private final Garcon garcon

  HttpExchangeHandler(Socket socket, Garcon garcon, Collection<Closeable> connections) {
    super(socket, garcon, connections)
    this.garcon = garcon
  }

  @Override
  protected HttpResponse processExchange(HttpRequest request) {
    EndpointDefinition endpointDefinition = garcon.endpointsHandler.getMatchingEndpointDefinition(request.path)
    if (endpointDefinition == null) {
      return new HttpResponse().tap {
        statusCode = 404
        message = 'Not Found'
      }
    } else if (endpointDefinition.method != request.method) {
      return new HttpResponse().tap {
        statusCode = 405
        message = 'Method Not Allowed'
      }
    }

    HttpResponse response = new HttpResponse().tap {
      statusCode = 200
      message = 'Ok'
    }

    endpointDefinition.rehydrate(new HttpExchangeContext(request, response, garcon.composers))
    try {
      Object returnValue = endpointDefinition.call()
      if (response.body == null && returnValue != null) {
        ContentType contentType = endpointDefinition.contentType ?: garcon.contentType
        if (contentType != null) {
          response.headers.putContentTypeHeader(contentType.headerValue)
          def composer = garcon.composers[contentType]
          if (composer) {
            returnValue = composer.call(returnValue)
          }
        }
        response.body = returnValue
      }
      return response
    } catch (Exception e) {
      e.printStackTrace()
      return new HttpResponse().tap {
        statusCode = 500
        message = 'Internal Server Error'
      }
    }
  }

}
