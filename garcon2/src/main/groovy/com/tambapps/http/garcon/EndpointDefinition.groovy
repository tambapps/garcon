package com.tambapps.http.garcon

import com.tambapps.http.garcon.exception.ParsingException
import groovy.transform.PackageScope

import java.nio.file.Path
import java.nio.file.Paths

@PackageScope
class EndpointDefinition {

  private Closure closure
  ContentType accept
  ContentType contentType

  HttpResponse call(HttpExchangeContext context) {
    // rehydrating
    closure.delegate = context
    closure.resolveStrategy = Closure.DELEGATE_FIRST
    HttpResponse response = context.response
    try {
      Object returnValue = closure.call()
      if (response.body == null && returnValue != null) {
        ContentType contentType = context.contentType
        if (contentType != null) {
          response.headers.putContentTypeHeader(contentType.headerValue)
          def composer = context.composers[contentType]
          if (composer) {
            returnValue = composer.call(returnValue)
          }
        }
        response.body = returnValue
      }
      return response
    } catch (ParsingException e) {
      return new HttpResponse(statusCode: HttpStatus.BAD_REQUEST)
    }
  }

}
