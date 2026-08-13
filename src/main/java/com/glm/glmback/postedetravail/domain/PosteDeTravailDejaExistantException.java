package com.glm.glmback.postedetravail.domain;

public final class PosteDeTravailDejaExistantException extends RuntimeException {

  public PosteDeTravailDejaExistantException(PosteDeTravailId id) {
    super("Le poste de travail %s existe deja".formatted(id.uuid()));
  }
}
