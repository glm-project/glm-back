package com.glm.glmback.atelier.domain;

public final class JourneeDeTravailDejaOuverteException extends RuntimeException {

  public JourneeDeTravailDejaOuverteException(OperateurId operateur) {
    super("Une journee de travail est deja ouverte pour l'operateur %s".formatted(operateur.uuid()));
  }
}
