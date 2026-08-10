package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.UUID;

public record EvenementDAtelierId(UUID uuid) implements Comparable<EvenementDAtelierId> {
  public EvenementDAtelierId {
    Assert.notNull("id de l'evenement d'atelier", uuid);
  }

  public static EvenementDAtelierId newId() {
    return new EvenementDAtelierId(UUID.randomUUID());
  }

  @Override
  public int compareTo(EvenementDAtelierId other) {
    return uuid().compareTo(other.uuid());
  }
}
