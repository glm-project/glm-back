package com.glm.glmback.postedetravail.infrastructure.primary;

import com.glm.glmback.postedetravail.domain.LibelleDejaUtiliseException;
import com.glm.glmback.postedetravail.domain.PosteDeTravailIntrouvableException;
import com.glm.glmback.postedetravail.domain.PosteDeTravailPointeException;
import com.glm.glmback.postedetravail.domain.PosteDeTravailUtiliseException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE - 20_000)
class PosteDeTravailExceptionAdvice {

  @ExceptionHandler(PosteDeTravailIntrouvableException.class)
  ProblemDetail handlePosteDeTravailIntrouvable(PosteDeTravailIntrouvableException e) {
    return ErreurDePosteDeTravail.POSTE_DE_TRAVAIL_INTROUVABLE.problem(e);
  }

  @ExceptionHandler(LibelleDejaUtiliseException.class)
  ProblemDetail handleLibelleDejaUtilise(LibelleDejaUtiliseException e) {
    return ErreurDePosteDeTravail.LIBELLE_DEJA_UTILISE.problem(e);
  }

  @ExceptionHandler(PosteDeTravailPointeException.class)
  ProblemDetail handlePosteDeTravailPointe(PosteDeTravailPointeException e) {
    return ErreurDePosteDeTravail.POSTE_DE_TRAVAIL_POINTE.problem(e);
  }

  @ExceptionHandler(PosteDeTravailUtiliseException.class)
  ProblemDetail handlePosteDeTravailUtilise(PosteDeTravailUtiliseException e) {
    return ErreurDePosteDeTravail.POSTE_DE_TRAVAIL_UTILISE.problem(e);
  }
}
