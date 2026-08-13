package com.glm.glmback.operateur.domain;

public final class OperateurIntrouvableException extends RuntimeException {

  public OperateurIntrouvableException(OperateurId id) {
    super("L'operateur %s est introuvable".formatted(id.uuid()));
  }
}
