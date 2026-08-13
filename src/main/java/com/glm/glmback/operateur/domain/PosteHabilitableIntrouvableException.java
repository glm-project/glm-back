package com.glm.glmback.operateur.domain;

public final class PosteHabilitableIntrouvableException extends RuntimeException {

  public PosteHabilitableIntrouvableException(PosteHabilitableId id) {
    super("Le poste de travail %s est introuvable".formatted(id.uuid()));
  }
}
