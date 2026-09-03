package com.glm.glmback.feuilledetemps.infrastructure.primary;

import com.glm.glmback.feuilledetemps.domain.OperateurInconnuException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE - 20_000)
class FeuilleDeTempsExceptionAdvice {

  @ExceptionHandler(OperateurInconnuException.class)
  ProblemDetail handleOperateurInconnu(OperateurInconnuException e) {
    return ErreurDeFeuilleDeTemps.OPERATEUR_INTROUVABLE.problem(e);
  }
}
