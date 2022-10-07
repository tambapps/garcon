package com.tambapps.http.garcon.util;

import com.tambapps.http.garcon.HttpExchangeContext;
import groovy.lang.Closure;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ReflectMethodClosure extends Closure<Object> {
  private final Method method;

  public ReflectMethodClosure(Object owner, Method method) {
    super(owner);
    this.method = method;
  }

  public Object doCall(HttpExchangeContext context)
      throws InvocationTargetException, IllegalAccessException {
    if (method.getParameterCount() == 0) {
      return method.invoke(getOwner());
    } else {
      return method.invoke(getOwner(), context);
    }
  }

}
