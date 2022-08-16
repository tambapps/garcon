package com.tambapps.http.garcon.util

import java.util.function.Consumer

class ClassUtils {

  static Class<?> getClassOrNull(String name) {
    try {
      return Class.forName(name)
    } catch (ClassNotFoundException e) {
      return null
    }
  }

  static void doIfClassExists(String name, Consumer<Class<?>> consumer) {
    Class<?> clazz = getClassOrNull(name)
    if (clazz != null) {
      consumer.accept(clazz)
    }
  }

}
