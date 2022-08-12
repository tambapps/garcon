package com.tambapps.http.garcon

import com.tambapps.http.garcon.io.parser.HttpRequestParser
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
  private ServerSocketChannel serverSocket

  void stop() {
    running.set(false)
    serverSocket.close()
  }

  void start() {
    running.set(true)
    Selector selector = Selector.open()
    serverSocket = ServerSocketChannel.open()
    serverSocket.bind(new InetSocketAddress("localhost", 8081))
    // Put the ServerSocketChannel into non-blocking mode
    serverSocket.configureBlocking(false)
    // Now register the channel with the Selector. The SelectionKey
    // represents the registration of this channel with this Selector.
    serverSocket.register(selector, SelectionKey.OP_ACCEPT)

    while (running.get()) {
      selector.select()
      Set<SelectionKey> selectedKeys = selector.selectedKeys()
      Iterator<SelectionKey> iter = selectedKeys.iterator()
      while (iter.hasNext()) {

        SelectionKey key = iter.next()

        if (key.isAcceptable()) {
          accept(selector, serverSocket)
        } else if (key.isReadable()) {
          read(key)
        } else if (key.isWritable()) {
          write(key)
        }
        iter.remove()
      }
    }
  }

  private void accept(Selector selector, ServerSocketChannel serverSocket) {
    SocketChannel client = serverSocket.accept()
    client.configureBlocking(false)
    client.register(selector, SelectionKey.OP_READ, new HttpAttachment())
  }

  private void read(SelectionKey selectionKey) {
    // for now we assume the buffer is big enough to read whole request
    SocketChannel ch = (SocketChannel) selectionKey.channel()
    buffer.clear()
    int read = ch.read(buffer)
    if (read == -1) {
      DefaultGroovyMethods.closeQuietly(selectionKey.channel().close())
    } else if (read > 0) {
      buffer.flip()
      HttpAttachment attachment = (HttpAttachment) selectionKey.attachment()
      attachment.getRequestParser().parse(buffer)
      // TODO add attachement and read
    }
    write(selectionKey)
  }

  private void write(SelectionKey selectionKey) {
    SocketChannel channel = (SocketChannel) selectionKey.channel()
    channel.write(    ByteBuffer.wrap((    "HTTP/1.0 200 OK\r\n" +
        "Date: Fri, 31 Dec 1999 23:59:59 GMT\r\n" +
        "Server: Apache/0.8.4\r\n" +
        "Content-Type: text/html\r\n" +
        "Content-Length: 59\r\n" +
        "Expires: Sat, 01 Jan 2000 00:59:59 GMT\r\n" +
        "Last-modified: Fri, 09 Aug 1996 14:21:40 GMT\r\n" +
        "\r\n" +
        "<TITLE>Exemple</TITLE>\r\n" +
        "<P>Ceci est une page d'exemple.</P>"
    ).bytes)
    )
    channel.close()
  }

  boolean isRunning() {
    return running.get()
  }

}
