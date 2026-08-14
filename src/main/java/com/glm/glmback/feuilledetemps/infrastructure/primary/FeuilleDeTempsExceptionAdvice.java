package com.glm.glmback.feuilledetemps.infrastructure.primary;

import com.glm.glmback.feuilledetemps.domain.OperateurInconnuException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE - 20_000)
class FeuilleDeTempsExceptionAdvice {

  private static final String MESSAGE_KEY = "message";

  @ExceptionHandler(OperateurInconnuException.class)
  ProblemDetail handleOperateurInconnu(OperateurInconnuException e) {
    var detail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
    detail.setTitle("operateur introuvable");
    detail.setProperty(MESSAGE_KEY, e.getMessage());

    return detail;
  }
}
