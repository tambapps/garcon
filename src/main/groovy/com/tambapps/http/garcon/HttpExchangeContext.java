package com.tambapps.http.garcon;

import com.tambapps.http.garcon.exception.ParsingException;
import com.tambapps.http.garcon.util.ContentTypeFunctionMap;
import groovy.lang.Delegate;
import groovy.transform.Generated;
import lombok.Data;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.function.Function;

/**
 * Context used for endpoint definition closures, as delegate
 */
@Data
public class HttpExchangeContext {

  // definition order matters because of @delegate
  @Delegate
  final HttpResponse response;
  @Delegate
  final HttpRequest request;
  final ContentTypeFunctionMap<Object, byte[]> composers;
  final ContentTypeFunctionMap<byte[], Object> parsers;
  final ContentType contentType;
  final ContentType accept;
  private Object parsedBody;

  HttpExchangeContext(HttpRequest request, HttpResponse response, ContentTypeFunctionMap<Object, byte[]> composers,
                      ContentTypeFunctionMap<byte[], Object> parsers, ContentType contentType, ContentType accept) {
    this.request = request;
    this.response = response;
    this.composers = composers;
    this.parsers = parsers;
    this.contentType = contentType;
    this.accept = accept;
  }

  Headers getRequestHeaders() {
    return request.getHeaders();
  }
  Headers getResponseHeaders() {
    return response.getHeaders();
  }

  public Object getParsedRequestBody() {
    return getParsedRequestBody(accept);
  }

  public Object getParsedRequestBody(ContentType accept) {
    if (request.getBody() == null) {
      return null;
    }
    if (this.parsedBody == null) {
      Object b;
      if (accept == null) {
        b = request.getBody();
      } else {
        Function<byte[], Object> parser = parsers.getAt(accept);
        if (parser != null) {
          try {
            b = parser.apply(request.getBody());
          } catch (Exception e) {
            throw new ParsingException(e);
          }
        } else {
          b = request.getBody();
        }
      }
      this.parsedBody = b;
    }
    return this.parsedBody;
  }


  @Generated
  public boolean is2xxSuccessful() {
    return this.response.is2xxSuccessful();
  }

  @Generated
  public void setBody(Object param0) {
    this.response.setBody(param0);
    Object var10000 = null;
  }

  @Generated
  public boolean isIndefiniteLength() {
    return this.response.isIndefiniteLength();
  }

  @Generated
  public Integer getContentLength() {
    return this.response.getContentLength();
  }

  @Generated
  public boolean isKeepAlive() {
    return this.response.isKeepAlive();
  }

  @Generated
  public String getHttpVersion() {
    return this.response.getHttpVersion();
  }

  @Generated
  public HttpStatusCode getStatusCode() {
    return this.response.getStatusCode();
  }

  @Generated
  public ByteBuffer getBody() {
    return this.response.getBody();
  }

  @Generated
  public void setStatusCode(HttpStatusCode param0) {
    this.response.setStatusCode(param0);
  }

  @Generated
  public String getMethod() {
    return this.request.getMethod();
  }

  @Generated
  public String getPath() {
    return this.request.getPath();
  }

  @Generated
  public Map<String, String> getQueryParams() {
    return this.request.getQueryParams();
  }


}

