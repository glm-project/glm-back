package com.glm.glmback.coutderevient.domain;

public final class TransitionDAtelierInterditeException extends RuntimeException {

  public TransitionDAtelierInterditeException(EvenementDAtelier evenement, EtatDActivite etat) {
    super("Un evenement d'atelier %s ne peut pas suivre l'etat %s".formatted(evenement.type(), etat));
  }
}
