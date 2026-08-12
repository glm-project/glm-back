package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.UUID;

public record SuiviDAtelierId(UUID uuid) implements Comparable<SuiviDAtelierId> {
  public SuiviDAtelierId {
    Assert.notNull("id du suivi d'atelier", uuid);
  }

  public static SuiviDAtelierId newId() {
    return new SuiviDAtelierId(UUID.randomUUID());
  }

  @Override
  public int compareTo(SuiviDAtelierId other) {
    return uuid().compareTo(other.uuid());
  }
}
