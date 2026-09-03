package com.glm.glmback.postedetravail.infrastructure.primary;

import com.glm.glmback.shared.error.infrastructure.primary.ProblemCode;
import org.springframework.http.HttpStatus;

enum ErreurDePosteDeTravail implements ProblemCode {
  POSTE_DE_TRAVAIL_INTROUVABLE(HttpStatus.NOT_FOUND, "poste de travail introuvable"),
  LIBELLE_DEJA_UTILISE(HttpStatus.CONFLICT, "libelle deja utilise"),
  POSTE_DE_TRAVAIL_POINTE(HttpStatus.CONFLICT, "poste de travail pointe"),
  POSTE_DE_TRAVAIL_UTILISE(HttpStatus.CONFLICT, "poste de travail utilise");

  private final HttpStatus status;
  private final String title;

  ErreurDePosteDeTravail(HttpStatus status, String title) {
    this.status = status;
    this.title = title;
  }

  @Override
  public HttpStatus status() {
    return status;
  }

  @Override
  public String title() {
    return title;
  }
}
