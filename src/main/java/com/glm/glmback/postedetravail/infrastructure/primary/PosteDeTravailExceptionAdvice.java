package com.glm.glmback.postedetravail.infrastructure.primary;

import com.glm.glmback.postedetravail.domain.LibelleDejaUtiliseException;
import com.glm.glmback.postedetravail.domain.PosteDeTravailIntrouvableException;
import com.glm.glmback.postedetravail.domain.PosteDeTravailPointeException;
import com.glm.glmback.postedetravail.domain.PosteDeTravailUtiliseException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE - 20_000)
class PosteDeTravailExceptionAdvice {

  private static final String MESSAGE_KEY = "message";

  @ExceptionHandler(PosteDeTravailIntrouvableException.class)
  ProblemDetail handlePosteDeTravailIntrouvable(PosteDeTravailIntrouvableException e) {
    var detail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
    detail.setTitle("poste de travail introuvable");
    detail.setProperty(MESSAGE_KEY, e.getMessage());

    return detail;
  }

  @ExceptionHandler(LibelleDejaUtiliseException.class)
  ProblemDetail handleLibelleDejaUtilise(LibelleDejaUtiliseException e) {
    var detail = ProblemDetail.forStatus(HttpStatus.CONFLICT);
    detail.setTitle("libelle deja utilise");
    detail.setProperty(MESSAGE_KEY, e.getMessage());

    return detail;
  }

  @ExceptionHandler(PosteDeTravailPointeException.class)
  ProblemDetail handlePosteDeTravailPointe(PosteDeTravailPointeException e) {
    var detail = ProblemDetail.forStatus(HttpStatus.CONFLICT);
    detail.setTitle("poste de travail pointe");
    detail.setProperty(MESSAGE_KEY, e.getMessage());

    return detail;
  }

  @ExceptionHandler(PosteDeTravailUtiliseException.class)
  ProblemDetail handlePosteDeTravailUtilise(PosteDeTravailUtiliseException e) {
    var detail = ProblemDetail.forStatus(HttpStatus.CONFLICT);
    detail.setTitle("poste de travail utilise");
    detail.setProperty(MESSAGE_KEY, e.getMessage());

    return detail;
  }
}
