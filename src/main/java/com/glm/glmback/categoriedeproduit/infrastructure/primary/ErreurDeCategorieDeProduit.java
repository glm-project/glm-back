package com.glm.glmback.categoriedeproduit.infrastructure.primary;

import com.glm.glmback.shared.error.infrastructure.primary.ProblemCode;
import org.springframework.http.HttpStatus;

enum ErreurDeCategorieDeProduit implements ProblemCode {
  CATEGORIE_DEJA_EXISTANTE(HttpStatus.CONFLICT, "categorie deja existante");

  private final HttpStatus status;
  private final String title;

  ErreurDeCategorieDeProduit(HttpStatus status, String title) {
    this.status = status;
    this.title = title;
  }

  @Override
  public String context() {
    return "categorie-de-produit";
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
