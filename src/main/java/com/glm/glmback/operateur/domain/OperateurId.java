package com.glm.glmback.operateur.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.UUID;

public record OperateurId(UUID uuid) implements Comparable<OperateurId> {
  public OperateurId {
    Assert.notNull("id de l'operateur", uuid);
  }

  public static OperateurId newId() {
    return new OperateurId(UUID.randomUUID());
  }

  @Override
  public int compareTo(OperateurId other) {
    return uuid().compareTo(other.uuid());
  }
}
