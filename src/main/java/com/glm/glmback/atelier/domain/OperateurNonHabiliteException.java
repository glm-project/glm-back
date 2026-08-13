package com.glm.glmback.atelier.domain;

public final class OperateurNonHabiliteException extends RuntimeException {

  public OperateurNonHabiliteException(OperateurId operateur, PosteDeTravailId poste) {
    super("L'operateur %s n'est pas habilite sur le poste de travail %s".formatted(operateur.uuid(), poste.uuid()));
  }
}
