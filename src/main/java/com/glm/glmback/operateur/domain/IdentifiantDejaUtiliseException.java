package com.glm.glmback.operateur.domain;

public final class IdentifiantDejaUtiliseException extends RuntimeException {

  public IdentifiantDejaUtiliseException(Identifiant identifiant) {
    super("L'identifiant %s est deja utilise par un autre operateur".formatted(identifiant.value()));
  }
}
