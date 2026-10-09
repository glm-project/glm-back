package com.glm.glmback.parametrage.infrastructure.primary;

import com.glm.glmback.parametrage.domain.LogoIntrouvableException;
import com.glm.glmback.parametrage.domain.LogoInvalideException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE - 20_000)
class ParametrageExceptionAdvice {

  @ExceptionHandler(LogoInvalideException.class)
  ProblemDetail handleLogoInvalide(LogoInvalideException e) {
    return ErreurDeParametrage.LOGO_INVALIDE.problem(e);
  }

  @ExceptionHandler(LogoIntrouvableException.class)
  ProblemDetail handleLogoIntrouvable(LogoIntrouvableException e) {
    return ErreurDeParametrage.LOGO_INTROUVABLE.problem(e);
  }
}
