package com.tambapps.http.garcon

import groovy.transform.CompileStatic
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.ChannelPipeline
import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpRequestDecoder
import io.netty.handler.codec.http.HttpResponseEncoder
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler

import java.util.concurrent.atomic.AtomicReference

// this garcon doesn't handle path variables, because sun HttpServer only handle static paths
@CompileStatic
class NettyGarcon extends Garcon {

  private final AtomicReference<NettyServer> serverReference = new AtomicReference<>()

  NettyGarcon() {}

  NettyGarcon(String address, int port) {
    // don't know why, groovy complains about it when just using class name
    this((java.net.InetAddress) InetAddress.getByName(address), port)
  }

  NettyGarcon(InetAddress address, int port) {
    super()
    super.setAddress(address)
    super.setPort(port)
  }

  NettyGarcon(InetAddress address, int port, int backlog) {
    super()
    super.setAddress(address)
    super.setPort(port)
    super.setBacklog(backlog)
  }

  @Override
  boolean isRunning() {
    return serverReference.get() != null
  }

  @Override
  void doStart(EndpointsHandler endpointsHandler) {
    // TODO make number of threads configurable
    EventLoopGroup bossGroup = new NioEventLoopGroup();
    EventLoopGroup workerGroup = new NioEventLoopGroup();

    ServerBootstrap b = new ServerBootstrap()
    if (backlog) {
      b.option(ChannelOption.SO_BACKLOG, backlog)
      b.childOption(ChannelOption.SO_KEEPALIVE, true)
    }
    final Garcon garcon = this
    b.group(bossGroup, workerGroup)
        .channel(NioServerSocketChannel.class)
        //.handler(new LoggingHandler(LogLevel.INFO))
        .childHandler(new ChannelInitializer<SocketChannel>() {
          @Override
          protected void initChannel(SocketChannel ch) throws Exception {
            ChannelPipeline p = ch.pipeline()
            p.addLast(new HttpRequestDecoder())
            p.addLast(new HttpResponseEncoder())
            // to parse request bodies
            p.addLast(new HttpObjectAggregator(maxRequestBytes?.toInteger() ?: Integer.MAX_VALUE))
            p.addLast(new NettyHttpExchangeHandler(garcon: garcon, endpointsHandler: endpointsHandler))
          }
        })
    Channel channel = b.bind(address, port ?: 0).sync().channel()
    serverReference.set(new NettyServer(channel: channel, bossGroup: bossGroup, workerGroup: workerGroup))
  }

  @Override
  void startAsync() {
    start()
  }

  @Override
  void doStop() {
    NettyServer server = serverReference.get()
    if (server == null) {
      return
    }
    server.with {
      channel.close().sync()
      bossGroup.shutdownGracefully()
      workerGroup.shutdownGracefully()
    }
  }

  static class NettyServer {
    Channel channel
    EventLoopGroup bossGroup
    EventLoopGroup workerGroup
  }
}
