package com.glm.glmback.atelier.domain;

public final class OperateurDAtelierIntrouvableException extends RuntimeException {

  public OperateurDAtelierIntrouvableException(OperateurId id) {
    super("L'operateur %s est introuvable".formatted(id.uuid()));
  }
}
