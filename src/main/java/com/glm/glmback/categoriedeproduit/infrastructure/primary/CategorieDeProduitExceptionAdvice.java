package com.glm.glmback.categoriedeproduit.infrastructure.primary;

import com.glm.glmback.categoriedeproduit.domain.CategorieDejaExistanteException;
import com.glm.glmback.categoriedeproduit.domain.CategorieIntrouvableException;
import com.glm.glmback.categoriedeproduit.domain.CategorieUtiliseeException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE - 20_000)
class CategorieDeProduitExceptionAdvice {

  @ExceptionHandler(CategorieIntrouvableException.class)
  ProblemDetail handleCategorieIntrouvable(CategorieIntrouvableException e) {
    return ErreurDeCategorieDeProduit.CATEGORIE_INTROUVABLE.problem(e);
  }

  @ExceptionHandler(CategorieDejaExistanteException.class)
  ProblemDetail handleCategorieDejaExistante(CategorieDejaExistanteException e) {
    return ErreurDeCategorieDeProduit.CATEGORIE_DEJA_EXISTANTE.problem(e);
  }

  @ExceptionHandler(CategorieUtiliseeException.class)
  ProblemDetail handleCategorieUtilisee(CategorieUtiliseeException e) {
    return ErreurDeCategorieDeProduit.CATEGORIE_UTILISEE.problem(e);
  }
}
