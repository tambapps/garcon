package com.tambapps.http.garcon.util;

import java.util.function.Consumer;

public class ClassUtils {

  /**
   * Util method to get class or null instead of throwing exception
   *
   * @param name the class name
   * @return the class or null
   */
  public static Class<?> getClassOrNull(String name) {
    try {
      return Class.forName(name);
    } catch (ClassNotFoundException e) {
      return null;
    }
  }

  /**
   * Do some action if the class exists
   *
   * @param name     the name of the class
   * @param consumer the consumer
   */
  public static void doIfClassExists(String name, Consumer<Class<?>> consumer) {
    Class<?> clazz = getClassOrNull(name);
    if (clazz != null) {
      consumer.accept(clazz);
    }
  }

}
