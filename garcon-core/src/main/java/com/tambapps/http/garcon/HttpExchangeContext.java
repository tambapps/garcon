package com.tambapps.http.garcon;

import com.tambapps.http.garcon.exception.ParsingException;
import com.tambapps.http.garcon.util.ContentTypeMap;
import lombok.Data;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.function.Function;

/**
 * Context used for endpoint definition closures, as delegate
 */
@Data
public abstract class HttpExchangeContext {

  // definition order matters because of @delegate
  final HttpResponse response;
  final HttpRequest request;
  final ContentTypeMap<Function<Object, byte[]>> composers;
  final ContentTypeMap<Function<byte[], Object>> parsers;
  final ContentType contentType;
  final ContentType accept;
  private Object parsedBody;

  private Map<String, String> pathVariables;

  HttpExchangeContext(HttpRequest request, HttpResponse response, ContentTypeMap<Function<Object, byte[]>> composers,
                      ContentTypeMap<Function<byte[], Object>> parsers, ContentType contentType, ContentType accept) {
    this.request = request;
    this.response = response;
    this.composers = composers;
    this.parsers = parsers;
    this.contentType = contentType;
    this.accept = accept;
  }

  /**
   * Returns the request headers
   * @return the request headers
   */
  Headers getRequestHeaders() {
    return request.getHeaders();
  }

  /**
   * Returns the response headers
   * @return the response headers
   */
  Headers getResponseHeaders() {
    return response.getHeaders();
  }

  /**
   * Returns the parsed request body, based on the content type
   * @return the parsed request body
   */
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


  /**
   * Returns whether the response is 2XX successful
   * @return whether the response is 2XX successful
   */
  public boolean is2xxSuccessful() {
    return this.response.is2xxSuccessful();
  }

  /**
   * Sets the response body
   * @param body the body to set
   */
  public void setBody(Object body) {
    this.response.setBody(body);
  }

  /**
   * Returns whether the response is indefinite length
   * @return whether the response is indefinite length
   */
  public boolean isIndefiniteLength() {
    return this.response.isIndefiniteLength();
  }

  /**
   * Returns the response's content length
   * @return the response's content length
   */
  public Integer getContentLength() {
    return this.response.getContentLength();
  }

  /**
   * Returns the whether the response is keep alive
   * @return the whether the response is keep alive
   */
  public boolean isKeepAlive() {
    return this.response.isKeepAlive();
  }

  /**
   * Returns the response's HTTP version
   * @return the response's HTTP version
   */
  public String getHttpVersion() {
    return this.response.getHttpVersion();
  }

  /**
   * Returns the response's status code
   * @return the response's status code
   */
  public HttpStatusCode getStatusCode() {
    return this.response.getStatusCode();
  }

  /**
   * Returns the response's body
   * @return the response's body
   */
  public ByteBuffer getBody() {
    return this.response.getBody();
  }

  /**
   * Returns the response's status code
   *
   * @param statusCode the response's status code
   */
  public void setStatusCode(HttpStatusCode statusCode) {
    this.response.setStatusCode(statusCode);
  }

  /**
   * Returns the request's method
   *
   * @return the request's method
   */
  public String getMethod() {
    return this.request.getMethod();
  }

  /**
   * Returns the request's path
   *
   * @return the request's path
   */
  public String getPath() {
    return this.request.getPath();
  }

  /**
   * Returns request's query params
   *
   * @return request's query params
   */
  public Map<String, String> getQueryParams() {
    return this.request.getQueryParams();
  }

}

