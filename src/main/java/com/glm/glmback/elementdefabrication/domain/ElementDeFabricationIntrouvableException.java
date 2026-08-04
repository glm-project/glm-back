package com.glm.glmback.elementdefabrication.domain;

public final class ElementDeFabricationIntrouvableException extends RuntimeException {

  public ElementDeFabricationIntrouvableException(ElementDeFabricationId id) {
    super("L'element de fabrication %s est introuvable".formatted(id.uuid()));
  }
}
