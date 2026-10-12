package com.glm.glmback.syntheseheures.infrastructure.primary;

import com.glm.glmback.syntheseheures.domain.EvaluationFutureException;
import com.glm.glmback.syntheseheures.domain.OperateurInconnuException;
import com.glm.glmback.syntheseheures.domain.PeriodeFutureException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE - 20_000)
class SyntheseDesHeuresExceptionAdvice {

  @ExceptionHandler(EvaluationFutureException.class)
  ProblemDetail handleEvaluationFuture(EvaluationFutureException e) {
    return ErreurDeSyntheseDesHeures.EVALUATION_FUTURE.problem(e);
  }

  @ExceptionHandler(OperateurInconnuException.class)
  ProblemDetail handleOperateurInconnu(OperateurInconnuException e) {
    return ErreurDeSyntheseDesHeures.OPERATEUR_INTROUVABLE.problem(e);
  }

  @ExceptionHandler(PeriodeFutureException.class)
  ProblemDetail handlePeriodeFuture(PeriodeFutureException e) {
    return ErreurDeSyntheseDesHeures.PERIODE_FUTURE.problem(e);
  }
}
