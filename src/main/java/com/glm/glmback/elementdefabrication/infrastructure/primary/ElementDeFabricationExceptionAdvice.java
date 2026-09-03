package com.glm.glmback.elementdefabrication.infrastructure.primary;

import com.glm.glmback.elementdefabrication.domain.ElementDeFabricationIntrouvableException;
import com.glm.glmback.elementdefabrication.domain.ReferenceDejaUtiliseeException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE - 20_000)
class ElementDeFabricationExceptionAdvice {

  @ExceptionHandler(ElementDeFabricationIntrouvableException.class)
  ProblemDetail handleElementDeFabricationIntrouvable(ElementDeFabricationIntrouvableException e) {
    return ErreurDElementDeFabrication.ELEMENT_DE_FABRICATION_INTROUVABLE.problem(e);
  }

  @ExceptionHandler(ReferenceDejaUtiliseeException.class)
  ProblemDetail handleReferenceDejaUtilisee(ReferenceDejaUtiliseeException e) {
    return ErreurDElementDeFabrication.REFERENCE_DEJA_UTILISEE.problem(e);
  }
}
