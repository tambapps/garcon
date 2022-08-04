package com.tambapps.http.garcon.util

class ClassUtils {

  static Class<?> getClassOrNull(String name) {
    try {
      return Class.forName(name)
    } catch (ClassNotFoundException e) {
      return null
    }
  }

  static void doIfClassExists(String name, Closure closure) {
    Class<?> clazz = getClassOrNull(name)
    if (clazz != null) {
      closure.call(clazz)
    }
  }

}
