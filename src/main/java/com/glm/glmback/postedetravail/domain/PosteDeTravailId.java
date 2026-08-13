package com.glm.glmback.postedetravail.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.UUID;

public record PosteDeTravailId(UUID uuid) implements Comparable<PosteDeTravailId> {
  public PosteDeTravailId {
    Assert.notNull("id du poste de travail", uuid);
  }

  public static PosteDeTravailId newId() {
    return new PosteDeTravailId(UUID.randomUUID());
  }

  @Override
  public int compareTo(PosteDeTravailId other) {
    return uuid().compareTo(other.uuid());
  }
}
