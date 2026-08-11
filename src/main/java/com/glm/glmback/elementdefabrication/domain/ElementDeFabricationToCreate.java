package com.glm.glmback.elementdefabrication.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.Optional;

public record ElementDeFabricationToCreate(
  TypeDElementDeFabrication type,
  Optional<Reference> reference,
  Optional<Description> description
) {
  public ElementDeFabricationToCreate {
    Assert.notNull("type", type);
    Assert.notNull("reference", reference);
    Assert.notNull("description", description);
  }

  public ElementDeFabricationToCreate(TypeDElementDeFabrication type, String reference, String description) {
    this(type, Reference.of(reference), Description.of(description));
  }
}
