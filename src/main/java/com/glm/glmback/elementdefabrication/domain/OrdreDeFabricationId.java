package com.glm.glmback.elementdefabrication.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.UUID;

public record OrdreDeFabricationId(UUID uuid) implements ElementDeFabricationId {
  public OrdreDeFabricationId {
    Assert.notNull("id", uuid);
  }

  public static OrdreDeFabricationId newId() {
    return new OrdreDeFabricationId(UUID.randomUUID());
  }
}
