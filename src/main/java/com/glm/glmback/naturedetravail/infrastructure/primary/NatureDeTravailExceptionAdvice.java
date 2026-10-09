package com.glm.glmback.naturedetravail.infrastructure.primary;

import com.glm.glmback.naturedetravail.domain.NatureDejaExistanteException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE - 20_000)
class NatureDeTravailExceptionAdvice {

  @ExceptionHandler(NatureDejaExistanteException.class)
  ProblemDetail handleNatureDejaExistante(NatureDejaExistanteException e) {
    return ErreurDeNatureDeTravail.NATURE_DEJA_EXISTANTE.problem(e);
  }
}
