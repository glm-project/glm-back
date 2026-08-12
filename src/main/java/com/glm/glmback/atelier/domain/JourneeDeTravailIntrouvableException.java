package com.glm.glmback.atelier.domain;

public final class JourneeDeTravailIntrouvableException extends RuntimeException {

  public JourneeDeTravailIntrouvableException(JourneeDeTravailId id) {
    super("La journee de travail %s est introuvable".formatted(id.uuid()));
  }
}
