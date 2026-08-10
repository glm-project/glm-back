package com.glm.glmback.atelier.domain;

public final class EvenementDePresenceDejaAnnuleException extends RuntimeException {

  public EvenementDePresenceDejaAnnuleException(EvenementDePresenceId id) {
    super("L'evenement de presence %s est deja annule".formatted(id.uuid()));
  }
}
