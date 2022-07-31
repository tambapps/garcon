package com.tambapps.http.garcon

import com.tambapps.http.garcon.exception.ComposingException
import com.tambapps.http.garcon.exception.ParsingException
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
        statusCode = HttpStatus.NOT_FOUND
      }
    } else if (endpointDefinition.method != request.method) {
      return new HttpResponse().tap {
        statusCode = HttpStatus.METHOD_NOT_ALLOWED
      }
    }

    HttpResponse response = new HttpResponse().tap {
      statusCode = HttpStatus.OK
    }

    endpointDefinition.rehydrate(new HttpExchangeContext(request, response, garcon.composers, garcon.parsers, endpointDefinition.accept ?: garcon.accept))
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
    } catch (ParsingException e) {
      return new HttpResponse().tap {
        statusCode = HttpStatus.BAD_REQUEST
      }
    } catch (Exception e) {
      // same behaviour for Composing Exception
      e.printStackTrace()
      return new HttpResponse().tap {
        statusCode = HttpStatus.INTERNAL_SERVER_ERROR
      }
    }
  }

}
