package com.glm.glmback.parametrage.infrastructure.primary;

import com.glm.glmback.shared.error.infrastructure.primary.ProblemCode;
import org.springframework.http.HttpStatus;

enum ErreurDeParametrage implements ProblemCode {
  LOGO_INVALIDE(HttpStatus.BAD_REQUEST, "logo invalide");

  private final HttpStatus status;
  private final String title;

  ErreurDeParametrage(HttpStatus status, String title) {
    this.status = status;
    this.title = title;
  }

  @Override
  public String context() {
    return "parametrage";
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
