package com.glm.glmback.coutderevient.infrastructure.primary;

import com.glm.glmback.coutderevient.domain.ElementInconnuException;
import com.glm.glmback.coutderevient.domain.RapportNonExportableException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE - 20_000)
class CoutDeRevientExceptionAdvice {

  @ExceptionHandler(ElementInconnuException.class)
  ProblemDetail handleElementInconnu(ElementInconnuException e) {
    return ErreurDeCoutDeRevient.ELEMENT_DE_FABRICATION_INTROUVABLE.problem(e);
  }

  @ExceptionHandler(RapportNonExportableException.class)
  ProblemDetail handleRapportNonExportable(RapportNonExportableException e) {
    return ErreurDeCoutDeRevient.RAPPORT_NON_EXPORTABLE.problem(e);
  }
}
