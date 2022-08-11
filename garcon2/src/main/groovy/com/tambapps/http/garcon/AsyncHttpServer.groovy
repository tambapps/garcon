package com.tambapps.http.garcon

import org.codehaus.groovy.runtime.DefaultGroovyMethods

import java.nio.ByteBuffer
import java.nio.channels.ClosedSelectorException
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.util.concurrent.atomic.AtomicBoolean

class AsyncHttpServer {

  private final AtomicBoolean running = new AtomicBoolean(false)
  private final ByteBuffer buffer = ByteBuffer.allocateDirect(1024 * 64 - 1)

  void start() {
    running.set(false)
    Selector selector = Selector.open()
    ServerSocketChannel serverSocket = ServerSocketChannel.open()
    serverSocket.bind(new InetSocketAddress("localhost", 8081))
    // Put the ServerSocketChannel into non-blocking mode
    serverSocket.configureBlocking(false)
    // Now register the channel with the Selector. The SelectionKey
    // represents the registration of this channel with this Selector.
    serverSocket.register(selector, SelectionKey.OP_ACCEPT)

    while (running.get()) {
      try {
        selector.select()
        Set<SelectionKey> selectedKeys = selector.selectedKeys()
        Iterator<SelectionKey> iter = selectedKeys.iterator()
        while (iter.hasNext()) {

          SelectionKey key = iter.next();

          if (key.isAcceptable()) {
            accept(selector, serverSocket)
          } else if (key.isReadable()) {
            read(key)
          } else if (key.isWritable()) {
            write(key)
          }
          iter.remove()
        }
      } catch (ClosedSelectorException e) {
        return
      }
    }
  }

  private void accept(Selector selector, ServerSocketChannel serverSocket) {
    SocketChannel client = serverSocket.accept()
    client.configureBlocking(false)
    client.register(selector, SelectionKey.OP_READ, new HttpAttachment())
  }

  private void read(SelectionKey selectionKey) {
    SocketChannel ch = (SocketChannel) selectionKey.channel()
    try {
      buffer.clear()
      int read = ch.read(buffer)
      if (read == -1) {
        closeKey(key, CLOSE_AWAY);
      } else if (read > 0) {
        buffer.flip()
        // TODO add attachement and read
      }

    }
    ByteBuffer buffer
    client.read(buffer)
    buffer.flip()
  }

  private void write(SelectionKey selectionKey) {

  }
  boolean isRunning() {
    return running.get()
  }

}
