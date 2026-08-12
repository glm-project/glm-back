package com.glm.glmback.atelier.domain;

public final class ElementEngageableIntrouvableException extends RuntimeException {

  public ElementEngageableIntrouvableException(ElementEngageId id) {
    super("L'element de fabrication %s est introuvable".formatted(id.uuid()));
  }
}
