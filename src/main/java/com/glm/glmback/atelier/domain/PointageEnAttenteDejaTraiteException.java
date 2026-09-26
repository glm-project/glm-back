package com.glm.glmback.atelier.domain;

public final class PointageEnAttenteDejaTraiteException extends RuntimeException {

  public PointageEnAttenteDejaTraiteException(PointageEnAttenteId id) {
    super("Le pointage en attente %s est deja traite".formatted(id.uuid()));
  }
}
