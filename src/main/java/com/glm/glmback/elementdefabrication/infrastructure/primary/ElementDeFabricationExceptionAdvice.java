package com.glm.glmback.elementdefabrication.infrastructure.primary;

import com.glm.glmback.elementdefabrication.domain.ElementDeFabricationIntrouvableException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE - 20_000)
class ElementDeFabricationExceptionAdvice {

  private static final String MESSAGE_KEY = "message";

  @ExceptionHandler(ElementDeFabricationIntrouvableException.class)
  ProblemDetail handleElementDeFabricationIntrouvable(ElementDeFabricationIntrouvableException e) {
    var detail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
    detail.setTitle("element de fabrication introuvable");
    detail.setProperty(MESSAGE_KEY, e.getMessage());

    return detail;
  }
}
