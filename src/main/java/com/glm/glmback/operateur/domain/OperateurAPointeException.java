package com.glm.glmback.operateur.domain;

public final class OperateurAPointeException extends RuntimeException {

  public OperateurAPointeException(OperateurId id) {
    super("L'operateur %s ne peut pas etre supprime : du temps est pointe a son nom".formatted(id.uuid()));
  }
}
