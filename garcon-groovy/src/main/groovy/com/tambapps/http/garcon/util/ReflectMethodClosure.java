package com.tambapps.http.garcon.util;

import com.tambapps.http.garcon.HttpExchangeContext;
import com.tambapps.http.garcon.HttpStatus;
import groovy.lang.Closure;
import lombok.SneakyThrows;

import java.lang.reflect.Method;

public class ReflectMethodClosure extends Closure<Object> {

  private final ReflectMethodInvoker reflectMethodInvoker;
  public ReflectMethodClosure(Object owner, Method method, HttpStatus status) {
    super(owner);
    this.reflectMethodInvoker = new ReflectMethodInvoker(owner, method, status);
  }

  @SneakyThrows
  public Object doCall(HttpExchangeContext context) {
    return reflectMethodInvoker.invoke(context);
  }

  // should improve performance (?), instead of relying on metaclass to
  @Override
  public Object call(Object... args) {
    return doCall((HttpExchangeContext) args[0]);
  }

  @Override
  public Object call(Object arguments) {
    return doCall((HttpExchangeContext) arguments);
  }
}
