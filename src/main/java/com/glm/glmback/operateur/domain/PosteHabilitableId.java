package com.glm.glmback.operateur.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.UUID;

public record PosteHabilitableId(UUID uuid) implements Comparable<PosteHabilitableId> {
  public PosteHabilitableId {
    Assert.notNull("id du poste", uuid);
  }

  @Override
  public int compareTo(PosteHabilitableId other) {
    return uuid().compareTo(other.uuid());
  }
}
