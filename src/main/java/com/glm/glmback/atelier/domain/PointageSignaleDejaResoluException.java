package com.glm.glmback.atelier.domain;

public final class PointageSignaleDejaResoluException extends RuntimeException {

  public PointageSignaleDejaResoluException(PointageSignaleId id) {
    super("Le pointage signale %s est deja resolu".formatted(id.uuid()));
  }
}
