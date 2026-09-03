package com.glm.glmback.operateur.infrastructure.primary;

import com.glm.glmback.operateur.domain.IdentiteDejaUtiliseeException;
import com.glm.glmback.operateur.domain.MatriculeDejaUtiliseException;
import com.glm.glmback.operateur.domain.OperateurAPointeException;
import com.glm.glmback.operateur.domain.OperateurIntrouvableException;
import com.glm.glmback.operateur.domain.PosteHabilitableIntrouvableException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE - 20_000)
class OperateurExceptionAdvice {

  @ExceptionHandler(OperateurIntrouvableException.class)
  ProblemDetail handleOperateurIntrouvable(OperateurIntrouvableException e) {
    return ErreurDOperateur.OPERATEUR_INTROUVABLE.problem(e);
  }

  @ExceptionHandler(PosteHabilitableIntrouvableException.class)
  ProblemDetail handlePosteHabilitableIntrouvable(PosteHabilitableIntrouvableException e) {
    return ErreurDOperateur.POSTE_DE_TRAVAIL_INTROUVABLE.problem(e);
  }

  @ExceptionHandler(OperateurAPointeException.class)
  ProblemDetail handleOperateurAPointe(OperateurAPointeException e) {
    return ErreurDOperateur.OPERATEUR_AYANT_POINTE.problem(e);
  }

  @ExceptionHandler(IdentiteDejaUtiliseeException.class)
  ProblemDetail handleIdentiteDejaUtilisee(IdentiteDejaUtiliseeException e) {
    return ErreurDOperateur.IDENTITE_DEJA_UTILISEE.problem(e);
  }

  @ExceptionHandler(MatriculeDejaUtiliseException.class)
  ProblemDetail handleMatriculeDejaUtilise(MatriculeDejaUtiliseException e) {
    return ErreurDOperateur.MATRICULE_DEJA_UTILISE.problem(e);
  }
}
