package com.tambapps.http.garcon

import com.tambapps.http.garcon.io.QueryParamParser
import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufInputStream
import io.netty.buffer.ByteBufOutputStream
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.DefaultFullHttpResponse
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.FullHttpResponse
import io.netty.handler.codec.http.HttpObject
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpRequest as NettyHttpRequest
import io.netty.handler.codec.http.HttpVersion

import static com.tambapps.http.garcon.Headers.CONNECTION_KEEP_ALIVE

@PackageScope
@CompileStatic
class NettyHttpExchangeHandler extends SimpleChannelInboundHandler<HttpObject> implements HttpExchangeHandler {

  private EndpointsHandler endpointsHandler

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
    if (msg instanceof NettyHttpRequest) {
      NettyHttpRequest req = (NettyHttpRequest) msg
      HttpRequest request = toGarconRequest(req)
      HttpResponse response = processExchange(request)

      ByteBuf responseBody
      if (response.body) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream()
        response.writeBody(bos)
        byte[] bytes = bos.toByteArray()
        responseBody = Unpooled.wrappedBuffer(bytes)
        if (response.body instanceof InputStream) {
          response.body = bytes
        }
      } else {
        responseBody = Unpooled.buffer(0)
      }
      FullHttpResponse nettyResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(response.statusCode.value),
          responseBody)
      addDefaultHeaders(request, response)
      def responseHeaders = nettyResponse.headers()
      response.headers.each { name, value -> responseHeaders.add(name, value) }

      ChannelFuture f = ctx.write(nettyResponse)
      if (!CONNECTION_KEEP_ALIVE.equalsIgnoreCase(response.getHeaders().getConnectionHeader())) {
        f.addListener(ChannelFutureListener.CLOSE)
      }
    }
  }

  private static HttpRequest toGarconRequest(NettyHttpRequest request) {
    URI uri = URI.create(request.uri())
    InputStream body = null
    if (request instanceof FullHttpRequest) {
      body = new ByteBufInputStream(((FullHttpRequest)request).content())
    }
    def headers = new Headers()
    request.headers().each { entry -> headers[entry.key] = entry.value }
    new HttpRequest(request.method().name(), uri.path, QueryParamParser.parseQueryParams(uri.query),
        request.protocolVersion().text(), headers.asImmutable(), body)
  }

  @Override
  List<EndpointDefinition> findPathEndpoints(String path) {
    return endpointsHandler.getDefinitionsForPath(path)
  }

  @Override
  void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    ctx.flush()
  }

  @Override
  void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    garcon.onConnectionUnexpectedError?.call(cause)
  }
}
