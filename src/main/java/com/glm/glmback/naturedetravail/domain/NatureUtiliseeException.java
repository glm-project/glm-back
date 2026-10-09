package com.glm.glmback.naturedetravail.domain;

public final class NatureUtiliseeException extends RuntimeException {

  public NatureUtiliseeException(NatureDeTravailId id) {
    super("La nature de travail %s ne peut pas etre supprimee : elle sert deja".formatted(id.uuid()));
  }
}
