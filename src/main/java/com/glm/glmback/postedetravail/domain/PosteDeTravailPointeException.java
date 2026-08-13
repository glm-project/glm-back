package com.glm.glmback.postedetravail.domain;

public final class PosteDeTravailPointeException extends RuntimeException {

  public PosteDeTravailPointeException(PosteDeTravailId id) {
    super("Le poste de travail %s ne peut pas etre supprime : du temps y a ete pointe".formatted(id.uuid()));
  }
}
