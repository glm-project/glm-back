package com.glm.glmback.atelier.domain;

public final class SuiviDAtelierDejaExistantException extends RuntimeException {

  public SuiviDAtelierDejaExistantException(SuiviDAtelierId id) {
    super("Le suivi d'atelier %s existe deja".formatted(id.uuid()));
  }
}
