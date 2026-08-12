package com.glm.glmback.atelier.domain;

public final class SuiviDAtelierIntrouvableException extends RuntimeException {

  public SuiviDAtelierIntrouvableException(SuiviDAtelierId id) {
    super("Le suivi d'atelier %s est introuvable".formatted(id.uuid()));
  }
}
