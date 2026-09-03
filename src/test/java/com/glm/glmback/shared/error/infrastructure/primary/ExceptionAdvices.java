package com.glm.glmback.shared.error.infrastructure.primary;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;

public final class ExceptionAdvices {

  private ExceptionAdvices() {}

  public static ProblemDetail translatedBy(Object advice, RuntimeException exception) {
    Method handler = handlerOf(advice.getClass(), exception.getClass());
    handler.setAccessible(true);

    try {
      return (ProblemDetail) handler.invoke(advice, exception);
    } catch (ReflectiveOperationException e) {
      throw new AssertionError(e);
    }
  }

  public static Set<Class<?>> exceptionsTranslatedBy(Class<?> advice) {
    return handlersOf(advice)
      .map(handler -> handler.getParameterTypes()[0])
      .collect(Collectors.toUnmodifiableSet());
  }

  private static Method handlerOf(Class<?> advice, Class<?> exception) {
    return handlersOf(advice)
      .filter(handler -> handler.getParameterTypes()[0].equals(exception))
      .findFirst()
      .orElseThrow(() -> new AssertionError("%s does not translate %s".formatted(advice.getSimpleName(), exception.getSimpleName())));
  }

  private static Stream<Method> handlersOf(Class<?> advice) {
    return Arrays.stream(advice.getDeclaredMethods()).filter(method -> method.isAnnotationPresent(ExceptionHandler.class));
  }
}
