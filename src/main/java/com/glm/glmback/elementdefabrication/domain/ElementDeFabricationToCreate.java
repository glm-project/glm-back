package com.glm.glmback.elementdefabrication.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.Optional;

public record ElementDeFabricationToCreate(Categorie categorie, Optional<Reference> reference, Optional<Description> description) {
  public ElementDeFabricationToCreate {
    Assert.notNull("categorie", categorie);
    Assert.notNull("reference", reference);
    Assert.notNull("description", description);
  }

  public ElementDeFabricationToCreate(String categorie, String reference, String description) {
    this(new Categorie(categorie), Reference.of(reference), Description.of(description));
  }
}
