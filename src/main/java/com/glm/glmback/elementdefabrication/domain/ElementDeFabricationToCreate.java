package com.glm.glmback.elementdefabrication.domain;

import com.glm.glmback.shared.error.domain.Assert;

public record ElementDeFabricationToCreate(TypeDElementDeFabrication type, Titre titre, Description description) {
  public ElementDeFabricationToCreate {
    Assert.notNull("type", type);
    Assert.notNull("titre", titre);
    Assert.notNull("description", description);
  }

  public ElementDeFabricationToCreate(TypeDElementDeFabrication type, String titre, String description) {
    this(type, new Titre(titre), new Description(description));
  }
}
