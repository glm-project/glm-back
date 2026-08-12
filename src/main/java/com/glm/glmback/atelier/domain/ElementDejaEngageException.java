package com.glm.glmback.atelier.domain;

public final class ElementDejaEngageException extends RuntimeException {

  public ElementDejaEngageException(ElementEngageId id) {
    super("L'element %s est deja engage dans l'atelier".formatted(id.uuid()));
  }
}
