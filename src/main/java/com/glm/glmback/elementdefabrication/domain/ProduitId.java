package com.glm.glmback.elementdefabrication.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.UUID;

public record ProduitId(UUID uuid) implements ElementDeFabricationId {
  public ProduitId {
    Assert.notNull("id", uuid);
  }

  public static ProduitId newId() {
    return new ProduitId(UUID.randomUUID());
  }
}
