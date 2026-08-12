package com.glm.glmback.atelier.domain;

public final class TransitionDAtelierInterditeException extends RuntimeException {

  public TransitionDAtelierInterditeException(EvenementDAtelier evenement, EtatDActivite etat) {
    super(
      "L'evenement %s de %s du %s est refuse : l'activite est %s".formatted(
        evenement.type(),
        evenement.operateur().value(),
        evenement.dateDeSurvenue(),
        etat
      )
    );
  }
}
