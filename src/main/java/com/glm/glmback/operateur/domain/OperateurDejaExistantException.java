package com.glm.glmback.operateur.domain;

public final class OperateurDejaExistantException extends RuntimeException {

  public OperateurDejaExistantException(OperateurId id) {
    super("L'operateur %s existe deja".formatted(id.uuid()));
  }
}
