package com.glm.glmback.parametrage.domain;

/**
 * Le parametrage est seme a la creation du schema de chaque entreprise : son absence est une corruption de la base,
 * jamais un cas metier.
 */
public final class ParametrageIntrouvableException extends RuntimeException {

  public ParametrageIntrouvableException() {
    super("Le parametrage de l'entreprise est introuvable");
  }
}
