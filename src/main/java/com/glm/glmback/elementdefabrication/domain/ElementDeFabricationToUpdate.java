package com.glm.glmback.elementdefabrication.domain;

import com.glm.glmback.shared.error.domain.Assert;

public record ElementDeFabricationToUpdate(ElementDeFabricationId id, Titre titre, Description description) {
  public ElementDeFabricationToUpdate {
    Assert.notNull("id", id);
    Assert.notNull("titre", titre);
    Assert.notNull("description", description);
  }

  public ElementDeFabricationToUpdate(ElementDeFabricationId id, String titre, String description) {
    this(id, new Titre(titre), new Description(description));
  }
}
