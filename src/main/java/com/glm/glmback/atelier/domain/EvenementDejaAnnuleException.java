package com.glm.glmback.atelier.domain;

public final class EvenementDejaAnnuleException extends RuntimeException {

  public EvenementDejaAnnuleException(EvenementDAtelierId id) {
    super("L'evenement d'atelier %s est deja annule".formatted(id.uuid()));
  }
}
