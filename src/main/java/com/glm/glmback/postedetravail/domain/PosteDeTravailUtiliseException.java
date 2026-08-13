package com.glm.glmback.postedetravail.domain;

public final class PosteDeTravailUtiliseException extends RuntimeException {

  public PosteDeTravailUtiliseException(PosteDeTravailId id) {
    super("Le poste de travail %s ne peut pas etre supprime : des operateurs y sont habilites".formatted(id.uuid()));
  }
}
