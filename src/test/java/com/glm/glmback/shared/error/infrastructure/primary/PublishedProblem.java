package com.glm.glmback.shared.error.infrastructure.primary;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.http.HttpStatus;

public record PublishedProblem(RuntimeException exception, String code, HttpStatus status, String title) {
  public static Set<Class<?>> exceptionsOf(Stream<PublishedProblem> problems) {
    return problems.map(PublishedProblem::exception).map(Object::getClass).collect(Collectors.toUnmodifiableSet());
  }
}
