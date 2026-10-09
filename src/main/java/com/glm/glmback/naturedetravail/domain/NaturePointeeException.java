package com.glm.glmback.naturedetravail.domain;

public final class NaturePointeeException extends RuntimeException {

  public NaturePointeeException(NatureDeTravailId id) {
    super("La nature de travail %s ne peut plus etre supprimee : du temps a ete pointe sous elle".formatted(id.uuid()));
  }
}
