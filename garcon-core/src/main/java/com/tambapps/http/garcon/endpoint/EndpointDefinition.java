package com.tambapps.http.garcon.endpoint;

import com.tambapps.http.garcon.ContentType;
import com.tambapps.http.garcon.HttpExchangeContext;
import com.tambapps.http.garcon.HttpResponse;
import lombok.Getter;

import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * The definition of an endpoint
 */
@Getter
public abstract class EndpointDefinition<T> {

  private final ContentType accept;
  private final ContentType contentType;

  /**
   * Constructs an endpoint definition
   *
   * @param accept             the accept content type (request)
   * @param contentType        the response content type
   */
  protected EndpointDefinition(ContentType accept, ContentType contentType) {
    this.accept = accept;
    this.contentType = contentType;
  }

  abstract Object doCall(HttpExchangeContext context);

  public HttpResponse call(HttpExchangeContext context) {
    Object returnValue = doCall(context);

    HttpResponse response = context.getResponse();
    if (response.getBody() == null && returnValue != null) {
      ContentType contentType = context.getContentType();
      if (contentType != null) {
        Function<Object, byte[]> composer = context.getComposers().getAt(contentType);
        if (composer != null) {
          returnValue = composer.apply(returnValue);
        }
      }
      response.setBody(returnValue);
    }
    if (context.getContentType() != null) {
      // using context's contentType because the definition CT might be null, and the garcon's might not be null
      response.getHeaders().putContentType(context.getContentType());
    }
    return response;
  }

  public abstract DynamicEndpointDefinition<T> toDynamic(List<String> pathVariableNames, Pattern pattern);

}
