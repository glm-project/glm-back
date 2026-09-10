package com.glm.glmback.syntheseheures.infrastructure.primary;

import com.glm.glmback.syntheseheures.domain.OperateurInconnuException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE - 20_000)
class SyntheseDesHeuresExceptionAdvice {

  @ExceptionHandler(OperateurInconnuException.class)
  ProblemDetail handleOperateurInconnu(OperateurInconnuException e) {
    return ErreurDeSyntheseDesHeures.OPERATEUR_INTROUVABLE.problem(e);
  }
}
