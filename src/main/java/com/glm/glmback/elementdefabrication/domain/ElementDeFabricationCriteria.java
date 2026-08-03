package com.glm.glmback.elementdefabrication.domain;

import com.glm.glmback.shared.error.domain.Assert;

public record ElementDeFabricationCriteria(Periode periode) {
  public ElementDeFabricationCriteria {
    Assert.notNull("periode", periode);
  }

  public boolean matches(ElementDeFabrication element) {
    return periode.contains(element.dateDeCreation());
  }
}
