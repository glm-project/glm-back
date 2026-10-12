package com.glm.glmback.atelier.domain;

public final class ElementDejaEngageException extends RuntimeException {

  public ElementDejaEngageException(ElementEngageId id) {
    super("L'element %s a deja ete engage dans l'atelier : un element cloture se rouvre".formatted(id.uuid()));
  }
}
