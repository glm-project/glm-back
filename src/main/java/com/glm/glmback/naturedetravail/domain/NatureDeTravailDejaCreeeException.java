package com.glm.glmback.naturedetravail.domain;

public final class NatureDeTravailDejaCreeeException extends RuntimeException {

  public NatureDeTravailDejaCreeeException(NatureDeTravailId id) {
    super("La nature de travail %s existe deja".formatted(id.uuid()));
  }
}
