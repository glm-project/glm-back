package com.glm.glmback.postedetravail.domain;

public final class NatureInconnueException extends RuntimeException {

  public NatureInconnueException(NatureDeTravailId id) {
    super("La nature de travail %s n'est pas declaree".formatted(id.uuid()));
  }
}
