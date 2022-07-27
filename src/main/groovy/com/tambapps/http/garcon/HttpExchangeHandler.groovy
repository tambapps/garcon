package com.tambapps.http.garcon

import com.tambapps.http.garcon.exception.RequestParsingException
import com.tambapps.http.garcon.io.RequestParser
import groovy.transform.PackageScope
import groovy.transform.TupleConstructor
import org.codehaus.groovy.runtime.DefaultGroovyMethods

import static com.tambapps.http.garcon.Garcon.CONNECTION_CLOSE
import static com.tambapps.http.garcon.Garcon.CONNECTION_HEADER
import static com.tambapps.http.garcon.Garcon.CONNECTION_KEEP_ALIVE

@PackageScope
@TupleConstructor
class HttpExchangeHandler implements Runnable {

  private static final RequestParser REQUEST_PARSER = new RequestParser()
  Socket socket
  Garcon garcon
  EndpointsHandler endpointsHandler
  Collection<Closeable> connections

  @Override
  void run() {
    try {
      InputStream inputStream = socket.inputStream
      OutputStream outputStream = socket.outputStream
      String connectionHeader = CONNECTION_KEEP_ALIVE
      while (garcon.running && connectionHeader.equalsIgnoreCase(CONNECTION_KEEP_ALIVE)) {
        HttpRequest request
        try {
          request = REQUEST_PARSER.parse(inputStream)
        } catch (RequestParsingException e) {
          newResponse(400, 'Bad Request', CONNECTION_CLOSE, 'Request is malformed'.bytes).writeInto(outputStream)
          continue
        }
        connectionHeader = request.headers[CONNECTION_HEADER] ?: CONNECTION_CLOSE
        String responseConnectionHeader = connectionHeader.equalsIgnoreCase(CONNECTION_KEEP_ALIVE) ? CONNECTION_KEEP_ALIVE : CONNECTION_CLOSE

        EndpointDefinition endpointDefinition = endpointsHandler.getMatchingEndpointDefinition(request.path)
        if (endpointDefinition.method != request.method) {
          newResponse(405, 'Method Not Allowed', CONNECTION_CLOSE,
              "Method ${request.method} is not allowed at this path".bytes).writeInto(outputStream)
          continue
        }
        HttpResponse response
        if (endpointDefinition != null) {
          response = newResponse(200, 'Ok', responseConnectionHeader, null)
          endpointDefinition.rehydrate(new HttpExchangeContext(request, response, garcon.composers))
          try {
            Object returnValue = endpointDefinition.call()
            if (response.body == null && returnValue != null) {
              ContentType contentType = endpointDefinition.contentType ?: garcon.contentType
              if (contentType != null) {
                def composer = garcon.composers[contentType]
                if (composer) {
                  returnValue = composer.call(returnValue)
                }
              }
              response.body = returnValue
            }
          } catch (Exception e) {
            e.printStackTrace()
            responseConnectionHeader = CONNECTION_CLOSE
            connectionHeader = CONNECTION_CLOSE
            response = newResponse(500, 'Internal Server Error', responseConnectionHeader, 'An internal server error occurred'.bytes)
          }
        } else {
          byte[] responseBody = "Resource at path ${request.path} was not found".bytes
          response = newResponse(404, 'Not Found', responseConnectionHeader, responseBody)
        }
        if (response.indefiniteSize) {
          response.headers['Connection'] = CONNECTION_CLOSE
          connectionHeader = CONNECTION_CLOSE
        } else {
          response.headers['Content-Length'] = response.contentSize
        }
        response.writeInto(outputStream)
      }
    } catch (EOFException|SocketException e) {
      // do nothing
    } catch (IOException e) {
      // TODO handle errors
      e.printStackTrace()
    } catch (Exception e) {
      // TODO handle errors
      e.printStackTrace()
    } finally {
      // closing socket will also close InputStream and OutputStream
      DefaultGroovyMethods.closeQuietly(socket)
      connections.remove(socket)
    }
  }

  private HttpResponse newResponse(int status, String message, String connection, byte[] body) {
    Map headers = [Connection: connection,
                   Server: 'Garcon (Tambapps)',
                   Date: 'Mon, 23 May 2005 22:38:34 GMT']
    if (body != null) {
      headers['Content-Length'] = body.size()
    }
    return new HttpResponse(httpVersion: 'HTTP/1.1', statusCode: status, message: message,
        headers: new Headers(headers),
        body: body)
  }
}
