package com.glm.glmback.atelier.domain;

public final class PointageEnAttenteDejaExistantException extends RuntimeException {

  public PointageEnAttenteDejaExistantException(PointageEnAttenteId id) {
    super("Le pointage en attente %s existe deja".formatted(id.uuid()));
  }
}
