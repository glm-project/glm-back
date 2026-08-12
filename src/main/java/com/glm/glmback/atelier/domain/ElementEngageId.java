package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.UUID;

public record ElementEngageId(UUID uuid) implements Comparable<ElementEngageId> {
  public ElementEngageId {
    Assert.notNull("id de l'element engage", uuid);
  }

  @Override
  public int compareTo(ElementEngageId other) {
    return uuid().compareTo(other.uuid());
  }
}
