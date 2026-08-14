package com.glm.glmback.feuilledetemps.domain;

public final class TransitionDePresenceInterditeException extends RuntimeException {

  public TransitionDePresenceInterditeException(EvenementDePresence evenement, EtatDePresence etat) {
    super("Un evenement de presence %s ne peut pas suivre l'etat %s".formatted(evenement.type(), etat));
  }
}
