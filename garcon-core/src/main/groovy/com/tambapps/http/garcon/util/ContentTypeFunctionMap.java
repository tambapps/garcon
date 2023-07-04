package com.tambapps.http.garcon.util;

import com.tambapps.http.garcon.ContentType;
import groovy.lang.Closure;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Function;

/**
 * Content type function map
 *
 * @param <T1> the type of the function arg
 * @param <T2> the returning type of the function
 */
public class ContentTypeFunctionMap<T1, T2> extends ContentTypeMap<Function<T1, T2>> {

  /**
   * Put a function for the provided content type
   *
   * @param key   the content type
   * @param value the value
   * @return the previous value for this content type, or null
   */
  public Function<T1, T2> put(ContentType key, Closure<T2> value) {
    return put(key, toFunction(value));
  }

  /**
   * Put a function for the provided content type
   *
   * @param contentType the content type
   * @param value       the value
   * @return the previous value for this content type, or null
   */
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

  /**
   * Sets the default value to use
   *
   * @param value the new default value
   */
  public void setDefaultValue(Closure<T2> value) {
    setDefaultValue(toFunction(value));
  }

  @AllArgsConstructor
  private class MethodFunction implements Function<T1, T2> {
    private final Method method;
    private final Object obj;

    @SuppressWarnings("unchecked")
    @SneakyThrows
    @Override
    public T2 apply(T1 t1) {
      return (T2) method.invoke(obj, t1);
    }
  }
}
