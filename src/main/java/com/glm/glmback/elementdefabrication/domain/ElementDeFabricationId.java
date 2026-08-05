package com.glm.glmback.elementdefabrication.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.UUID;

public record ElementDeFabricationId(UUID uuid) implements Comparable<ElementDeFabricationId> {
  public ElementDeFabricationId {
    Assert.notNull("id", uuid);
  }

  public static ElementDeFabricationId newId() {
    return new ElementDeFabricationId(UUID.randomUUID());
  }

  @Override
  public int compareTo(ElementDeFabricationId other) {
    return uuid().compareTo(other.uuid());
  }
}
