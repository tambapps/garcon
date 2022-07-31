package com.tambapps.http.garcon;

import lombok.Getter;
import lombok.Setter;

import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;

public class JavaGarcon extends AbstractGarcon {

  private final Function<HttpRequest, HttpResponse> exchange;

  @Getter
  @Setter
  private Consumer<SocketException> onClosed;
  @Getter
  @Setter
  private Consumer<IOException> onError;
  @Getter
  @Setter
  private Consumer<IOException> onConnectionClosed;
  @Getter
  @Setter
  private Consumer<IOException> onConnectionError;
  @Getter
  @Setter
  private Consumer<Exception> onConnectionUnexpectedError;

  public JavaGarcon(Function<HttpRequest, HttpResponse> exchange) {
    this.exchange = exchange;
  }

  @Override
  Runnable newExchangeHandler(Socket socket, AbstractGarcon garcon,
      Collection<Closeable> connections) {
    return new JavaHttpExchangeHandler(socket, this, connections, exchange,
        onConnectionClosed, onConnectionError, onConnectionUnexpectedError);
  }

  @Override
  void onServerSocketClosed(SocketException e) {
    if (onClosed != null) {
      onClosed.accept(e);
    }
  }

  @Override
  void onServerException(IOException e) {
    if (onError != null) {
      onError.accept(e);
    }
  }
}
