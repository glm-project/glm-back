package com.glm.glmback.operateur.infrastructure.primary;

import com.glm.glmback.operateur.domain.IdentiteDejaUtiliseeException;
import com.glm.glmback.operateur.domain.MatriculeDejaUtiliseException;
import com.glm.glmback.operateur.domain.OperateurAPointeException;
import com.glm.glmback.operateur.domain.OperateurIntrouvableException;
import com.glm.glmback.operateur.domain.PosteHabilitableIntrouvableException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE - 20_000)
class OperateurExceptionAdvice {

  private static final String MESSAGE_KEY = "message";

  @ExceptionHandler(OperateurIntrouvableException.class)
  ProblemDetail handleOperateurIntrouvable(OperateurIntrouvableException e) {
    var detail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
    detail.setTitle("operateur introuvable");
    detail.setProperty(MESSAGE_KEY, e.getMessage());

    return detail;
  }

  @ExceptionHandler(PosteHabilitableIntrouvableException.class)
  ProblemDetail handlePosteHabilitableIntrouvable(PosteHabilitableIntrouvableException e) {
    var detail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
    detail.setTitle("poste de travail introuvable");
    detail.setProperty(MESSAGE_KEY, e.getMessage());

    return detail;
  }

  @ExceptionHandler(OperateurAPointeException.class)
  ProblemDetail handleOperateurAPointe(OperateurAPointeException e) {
    var detail = ProblemDetail.forStatus(HttpStatus.CONFLICT);
    detail.setTitle("operateur ayant pointe");
    detail.setProperty(MESSAGE_KEY, e.getMessage());

    return detail;
  }

  @ExceptionHandler(IdentiteDejaUtiliseeException.class)
  ProblemDetail handleIdentiteDejaUtilisee(IdentiteDejaUtiliseeException e) {
    var detail = ProblemDetail.forStatus(HttpStatus.CONFLICT);
    detail.setTitle("identite deja utilisee");
    detail.setProperty(MESSAGE_KEY, e.getMessage());

    return detail;
  }

  @ExceptionHandler(MatriculeDejaUtiliseException.class)
  ProblemDetail handleMatriculeDejaUtilise(MatriculeDejaUtiliseException e) {
    var detail = ProblemDetail.forStatus(HttpStatus.CONFLICT);
    detail.setTitle("matricule deja utilise");
    detail.setProperty(MESSAGE_KEY, e.getMessage());

    return detail;
  }
}
