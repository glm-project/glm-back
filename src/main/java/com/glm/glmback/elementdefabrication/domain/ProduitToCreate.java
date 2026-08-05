package com.glm.glmback.elementdefabrication.domain;

import com.glm.glmback.shared.error.domain.Assert;

public record ProduitToCreate(Titre titre, Description description) implements ElementDeFabricationToCreate {
  public ProduitToCreate {
    Assert.notNull("titre", titre);
    Assert.notNull("description", description);
  }

  public ProduitToCreate(String titre, String description) {
    this(new Titre(titre), new Description(description));
  }
}
