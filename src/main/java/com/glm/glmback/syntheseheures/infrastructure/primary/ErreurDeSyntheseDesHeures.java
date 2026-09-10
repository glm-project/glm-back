package com.glm.glmback.syntheseheures.infrastructure.primary;

import com.glm.glmback.shared.error.infrastructure.primary.ProblemCode;
import org.springframework.http.HttpStatus;

enum ErreurDeSyntheseDesHeures implements ProblemCode {
  OPERATEUR_INTROUVABLE(HttpStatus.NOT_FOUND, "operateur introuvable");

  private final HttpStatus status;
  private final String title;

  ErreurDeSyntheseDesHeures(HttpStatus status, String title) {
    this.status = status;
    this.title = title;
  }

  @Override
  public String context() {
    return "synthese-des-heures";
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
