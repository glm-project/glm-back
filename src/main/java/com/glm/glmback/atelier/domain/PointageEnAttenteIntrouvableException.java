package com.glm.glmback.atelier.domain;

public final class PointageEnAttenteIntrouvableException extends RuntimeException {

  public PointageEnAttenteIntrouvableException(PointageEnAttenteId id) {
    super("Aucun pointage en attente ne porte l'identifiant %s".formatted(id.uuid()));
  }
}
