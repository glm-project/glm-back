package com.glm.glmback.naturedetravail.infrastructure.primary;

import com.glm.glmback.shared.error.infrastructure.primary.ProblemCode;
import org.springframework.http.HttpStatus;

enum ErreurDeNatureDeTravail implements ProblemCode {
  NATURE_INTROUVABLE(HttpStatus.NOT_FOUND, "nature introuvable"),
  NATURE_DEJA_EXISTANTE(HttpStatus.CONFLICT, "nature deja existante"),
  NATURE_UTILISEE(HttpStatus.CONFLICT, "nature utilisee"),
  NATURE_POINTEE(HttpStatus.CONFLICT, "nature pointee");

  private final HttpStatus status;
  private final String title;

  ErreurDeNatureDeTravail(HttpStatus status, String title) {
    this.status = status;
    this.title = title;
  }

  @Override
  public String context() {
    return "nature-de-travail";
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
