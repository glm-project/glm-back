package com.glm.glmback.atelier.domain;

public final class PointageSignaleIntrouvableException extends RuntimeException {

  public PointageSignaleIntrouvableException(PointageSignaleId id) {
    super("Aucun pointage signale ne porte l'identifiant %s".formatted(id.uuid()));
  }
}
