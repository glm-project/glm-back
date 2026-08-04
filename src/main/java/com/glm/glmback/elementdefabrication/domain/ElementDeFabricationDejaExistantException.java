package com.glm.glmback.elementdefabrication.domain;

public final class ElementDeFabricationDejaExistantException extends RuntimeException {

  public ElementDeFabricationDejaExistantException(ElementDeFabricationId id) {
    super("L'element de fabrication %s existe deja".formatted(id.uuid()));
  }
}
