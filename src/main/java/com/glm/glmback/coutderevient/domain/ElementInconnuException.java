package com.glm.glmback.coutderevient.domain;

public final class ElementInconnuException extends RuntimeException {

  public ElementInconnuException(ElementId element) {
    super("L'element de fabrication %s n'existe pas".formatted(element.uuid()));
  }
}
