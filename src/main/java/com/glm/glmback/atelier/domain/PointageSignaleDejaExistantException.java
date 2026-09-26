package com.glm.glmback.atelier.domain;

public final class PointageSignaleDejaExistantException extends RuntimeException {

  public PointageSignaleDejaExistantException(PointageSignaleId id) {
    super("Le pointage %s est deja signale".formatted(id.uuid()));
  }
}
