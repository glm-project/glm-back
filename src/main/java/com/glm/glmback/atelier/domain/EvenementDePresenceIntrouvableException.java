package com.glm.glmback.atelier.domain;

public final class EvenementDePresenceIntrouvableException extends RuntimeException {

  public EvenementDePresenceIntrouvableException(EvenementDePresenceId id) {
    super("L'evenement de presence %s est introuvable".formatted(id.uuid()));
  }
}
