package com.glm.glmback.elementdefabrication.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.Optional;

public record ElementDeFabricationToUpdate(ElementDeFabricationId id, Optional<Reference> reference, Optional<Description> description) {
  public ElementDeFabricationToUpdate {
    Assert.notNull("id", id);
    Assert.notNull("reference", reference);
    Assert.notNull("description", description);
  }

  public ElementDeFabricationToUpdate(ElementDeFabricationId id, String reference, String description) {
    this(id, Reference.of(reference), Description.of(description));
  }
}
