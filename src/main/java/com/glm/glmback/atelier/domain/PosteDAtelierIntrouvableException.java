package com.glm.glmback.atelier.domain;

public final class PosteDAtelierIntrouvableException extends RuntimeException {

  public PosteDAtelierIntrouvableException(PosteDeTravailId id) {
    super("Le poste de travail %s est introuvable".formatted(id.uuid()));
  }
}
