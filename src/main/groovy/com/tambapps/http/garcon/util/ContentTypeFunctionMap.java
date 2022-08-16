package com.tambapps.http.garcon.util;

import com.tambapps.http.garcon.ContentType;
import groovy.lang.Closure;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Stream;

// using this because java lambda may be faster than groovy CLosure (?)
public class ContentTypeFunctionMap<T1, T2> extends ContentTypeMap<Function<T1, T2>> {

  public Function<T1, T2> put(ContentType key, Closure<T2> value) {
    return put(key, toFunction(value));
  }

  public Function<T1, T2> putAt(ContentType contentType, Closure<T2> value) {
    return putAt(contentType, toFunction(value));
  }

  private Function<T1, T2> toFunction(Closure<?
      > closure) {
    Method doCall = Arrays.stream(closure.getClass().getMethods())
        .filter(e -> e.getName().equals("doCall"))
        .findFirst()
        .get();
    return new MethodFunction(doCall, closure);
  }

  public void setDefaultValue(Closure<T2> value) {
    setDefaultValue(toFunction(value));
  }

  @AllArgsConstructor
  private class MethodFunction implements Function<T1, T2> {
    private final Method method;
    private final Object obj;

    @SneakyThrows
    @Override
    public T2 apply(T1 t1) {
      return (T2) method.invoke(obj, t1);
    }
  }
}
