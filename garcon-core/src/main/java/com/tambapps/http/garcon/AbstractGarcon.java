package com.tambapps.http.garcon;

import com.tambapps.http.garcon.util.IoUtils;
import lombok.Getter;
import lombok.Setter;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

abstract class AbstractGarcon {

  private ExecutorService executorService;
  private final AtomicBoolean running = new AtomicBoolean(false);
  private final Queue<Closeable> connections = new ConcurrentLinkedQueue<>();
  private ExecutorService requestsExecutorService;

  @Getter
  private InetAddress address;
  @Getter
  private Integer port;
  @Getter
  private Integer backlog;
  @Getter
  private int nbThreads = 4;
  @Getter
  @Setter
  private Long maxRequestBytes = null;

  abstract Runnable newExchangeHandler(Socket socket, AbstractGarcon garcon, Collection<Closeable> connections);

  abstract void onServerSocketClosed(SocketException e);
  abstract void onServerException(IOException e);

  public void start() {
    if (running.get()) {
      // already running
      return;
    }
    running.set(true);
    requestsExecutorService = Executors.newFixedThreadPool(nbThreads);
    try (ServerSocket serverSocket = newServerSocket()) {
      connections.add(serverSocket);
      while (running.get()) {
        Socket socket = serverSocket.accept();
        connections.add(socket);
        requestsExecutorService.submit(newExchangeHandler(socket, this, connections));
      }
    } catch (SocketException e) {
      // the socket was probably closed, do nothing
      onServerSocketClosed(e);
    } catch (IOException e) {
      onServerException(e);
    }
    running.set(false);
  }

  private ServerSocket newServerSocket() {
    int port = this.port != null ? this.port : 0;
    int backlog = this.backlog != null ? this.backlog : 0;
    try {
      if (address != null) {
        return new ServerSocket(port, backlog, address);
      } else {
        return new ServerSocket(port, backlog);
      }
    } catch (IOException e) {
      throw new RuntimeException("Couldn't start server socket: " + e.getMessage(), e);
    }
  }

  public boolean isRunning() {
    return running.get();
  }

  public void startAsync() {
    if (running.get()) {
      // already running
      return;
    }
    if (executorService == null) {
      executorService = Executors.newSingleThreadExecutor();
    }
    executorService.submit(new Runnable() {
      @Override
      public void run() {
        try {
          start();
        } catch (Exception e) {
          // shouldn't happen... but well...
          e.printStackTrace();
          running.set(false);
        }
      }
    });
  }

  public void stop() {
    running.set(false);
    if (requestsExecutorService != null) {
      requestsExecutorService.shutdown();
    }
    requestsExecutorService = null;

    if (executorService != null) {
      executorService.shutdown();
    }
    executorService = null;
    for (Closeable connection : connections) {
      IoUtils.closeQuietly(connection);
    }
    connections.clear();
  }

  public void setAddress(InetAddress address) {
    checkRunning("Cannot modify address while running");
    this.address = address;
  }

  public void setBacklog(Integer backlog) {
    checkRunning("Cannot modify backlog while running");
    this.backlog = backlog;
  }

  public void setPort(Integer port) {
    checkRunning("Cannot modify port while running");
    this.port = port;
  }

  public void setNbThreads(int nbThreads) {
    checkRunning("Cannot modify nbThreads while running");
    this.nbThreads = nbThreads;
  }

  private void checkRunning(String errorMessage) {
    if (isRunning()) {
      throw new IllegalStateException(errorMessage);
    }
  }
}
