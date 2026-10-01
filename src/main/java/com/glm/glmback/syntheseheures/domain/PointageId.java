package com.glm.glmback.syntheseheures.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.UUID;

/** Identite du fait conserve au journal. */
public record PointageId(UUID uuid) implements Comparable<PointageId> {
  public PointageId {
    Assert.notNull("id du pointage", uuid);
  }

  @Override
  public int compareTo(PointageId autre) {
    return uuid.compareTo(autre.uuid);
  }
}
