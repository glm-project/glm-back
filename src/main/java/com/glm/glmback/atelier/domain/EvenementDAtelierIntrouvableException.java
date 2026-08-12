package com.glm.glmback.atelier.domain;

public final class EvenementDAtelierIntrouvableException extends RuntimeException {

  public EvenementDAtelierIntrouvableException(EvenementDAtelierId id) {
    super("L'evenement d'atelier %s est introuvable".formatted(id.uuid()));
  }
}
