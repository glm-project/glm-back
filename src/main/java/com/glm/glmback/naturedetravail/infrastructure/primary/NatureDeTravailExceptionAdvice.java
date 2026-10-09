package com.glm.glmback.naturedetravail.infrastructure.primary;

import com.glm.glmback.naturedetravail.domain.NatureDejaExistanteException;
import com.glm.glmback.naturedetravail.domain.NatureIntrouvableException;
import com.glm.glmback.naturedetravail.domain.NaturePointeeException;
import com.glm.glmback.naturedetravail.domain.NatureUtiliseeException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE - 20_000)
class NatureDeTravailExceptionAdvice {

  @ExceptionHandler(NatureIntrouvableException.class)
  ProblemDetail handleNatureIntrouvable(NatureIntrouvableException e) {
    return ErreurDeNatureDeTravail.NATURE_INTROUVABLE.problem(e);
  }

  @ExceptionHandler(NatureDejaExistanteException.class)
  ProblemDetail handleNatureDejaExistante(NatureDejaExistanteException e) {
    return ErreurDeNatureDeTravail.NATURE_DEJA_EXISTANTE.problem(e);
  }

  @ExceptionHandler(NatureUtiliseeException.class)
  ProblemDetail handleNatureUtilisee(NatureUtiliseeException e) {
    return ErreurDeNatureDeTravail.NATURE_UTILISEE.problem(e);
  }

  @ExceptionHandler(NaturePointeeException.class)
  ProblemDetail handleNaturePointee(NaturePointeeException e) {
    return ErreurDeNatureDeTravail.NATURE_POINTEE.problem(e);
  }
}
