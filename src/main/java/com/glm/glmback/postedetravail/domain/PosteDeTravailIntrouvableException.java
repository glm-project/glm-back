package com.glm.glmback.postedetravail.domain;

public final class PosteDeTravailIntrouvableException extends RuntimeException {

  public PosteDeTravailIntrouvableException(PosteDeTravailId id) {
    super("Le poste de travail %s est introuvable".formatted(id.uuid()));
  }
}
