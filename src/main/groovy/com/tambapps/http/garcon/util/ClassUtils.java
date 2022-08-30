package com.tambapps.http.garcon.util;

import java.util.function.Consumer;

public class ClassUtils {

  public static Class<?> getClassOrNull(String name) {
    try {
      return Class.forName(name);
    } catch (ClassNotFoundException e) {
      return null;
    }
  }

  public static void doIfClassExists(String name, Consumer<Class<?>> consumer) {
    Class<?> clazz = getClassOrNull(name);
    if (clazz != null) {
      consumer.accept(clazz);
    }
  }

}
