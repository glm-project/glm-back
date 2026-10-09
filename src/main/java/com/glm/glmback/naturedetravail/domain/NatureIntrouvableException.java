package com.glm.glmback.naturedetravail.domain;

public final class NatureIntrouvableException extends RuntimeException {

  public NatureIntrouvableException(NatureDeTravailId id) {
    super("La nature de travail %s est introuvable".formatted(id.uuid()));
  }
}
