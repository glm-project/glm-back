package com.glm.glmback.naturedetravail.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.UUID;

public record NatureDeTravailId(UUID uuid) implements Comparable<NatureDeTravailId> {
  public NatureDeTravailId {
    Assert.notNull("id de la nature de travail", uuid);
  }

  public static NatureDeTravailId newId() {
    return new NatureDeTravailId(UUID.randomUUID());
  }

  @Override
  public int compareTo(NatureDeTravailId other) {
    return uuid().compareTo(other.uuid());
  }
}
