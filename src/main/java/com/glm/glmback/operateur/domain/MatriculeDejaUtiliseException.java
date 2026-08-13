package com.glm.glmback.operateur.domain;

public final class MatriculeDejaUtiliseException extends RuntimeException {

  public MatriculeDejaUtiliseException(Matricule matricule) {
    super("Le matricule %s est deja utilise par un autre operateur".formatted(matricule.value()));
  }
}
