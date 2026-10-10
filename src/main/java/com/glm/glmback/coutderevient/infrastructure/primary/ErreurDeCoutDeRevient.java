package com.glm.glmback.coutderevient.infrastructure.primary;

import com.glm.glmback.shared.error.infrastructure.primary.ProblemCode;
import org.springframework.http.HttpStatus;

enum ErreurDeCoutDeRevient implements ProblemCode {
  ELEMENT_DE_FABRICATION_INTROUVABLE(HttpStatus.NOT_FOUND, "element de fabrication introuvable");

  private final HttpStatus status;
  private final String title;

  ErreurDeCoutDeRevient(HttpStatus status, String title) {
    this.status = status;
    this.title = title;
  }

  @Override
  public String context() {
    return "cout-de-revient";
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
