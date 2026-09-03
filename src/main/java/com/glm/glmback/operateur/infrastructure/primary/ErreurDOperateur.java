package com.glm.glmback.operateur.infrastructure.primary;

import com.glm.glmback.shared.error.infrastructure.primary.ProblemCode;
import org.springframework.http.HttpStatus;

enum ErreurDOperateur implements ProblemCode {
  OPERATEUR_INTROUVABLE(HttpStatus.NOT_FOUND, "operateur introuvable"),
  POSTE_DE_TRAVAIL_INTROUVABLE(HttpStatus.NOT_FOUND, "poste de travail introuvable"),
  OPERATEUR_AYANT_POINTE(HttpStatus.CONFLICT, "operateur ayant pointe"),
  IDENTITE_DEJA_UTILISEE(HttpStatus.CONFLICT, "identite deja utilisee"),
  MATRICULE_DEJA_UTILISE(HttpStatus.CONFLICT, "matricule deja utilise");

  private final HttpStatus status;
  private final String title;

  ErreurDOperateur(HttpStatus status, String title) {
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
