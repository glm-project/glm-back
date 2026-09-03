package com.glm.glmback.elementdefabrication.infrastructure.primary;

import com.glm.glmback.shared.error.infrastructure.primary.ProblemCode;
import org.springframework.http.HttpStatus;

enum ErreurDElementDeFabrication implements ProblemCode {
  ELEMENT_DE_FABRICATION_INTROUVABLE(HttpStatus.NOT_FOUND, "element de fabrication introuvable"),
  REFERENCE_DEJA_UTILISEE(HttpStatus.CONFLICT, "reference deja utilisee");

  private final HttpStatus status;
  private final String title;

  ErreurDElementDeFabrication(HttpStatus status, String title) {
    this.status = status;
    this.title = title;
  }

  @Override
  public String context() {
    return "element-de-fabrication";
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
