package com.glm.glmback.feuilledetemps.domain;

public final class OperateurInconnuException extends RuntimeException {

  public OperateurInconnuException(OperateurId operateur) {
    super("L'operateur %s n'existe pas dans le referentiel".formatted(operateur.uuid()));
  }
}
