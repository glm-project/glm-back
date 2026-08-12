package com.glm.glmback.atelier.domain;

public final class SuiviDAtelierClotureException extends RuntimeException {

  public SuiviDAtelierClotureException(SuiviDAtelierId id) {
    super("Le suivi d'atelier %s est cloture".formatted(id.uuid()));
  }

  public SuiviDAtelierClotureException(EvenementDAtelier evenement) {
    super("L'evenement %s du %s est posterieur a la cloture du suivi".formatted(evenement.type(), evenement.dateDeSurvenue()));
  }
}
