package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.UUID;

public record EvenementDePresenceId(UUID uuid) implements Comparable<EvenementDePresenceId> {
  public EvenementDePresenceId {
    Assert.notNull("id de l'evenement de presence", uuid);
  }

  public static EvenementDePresenceId newId() {
    return new EvenementDePresenceId(UUID.randomUUID());
  }

  @Override
  public int compareTo(EvenementDePresenceId other) {
    return uuid().compareTo(other.uuid());
  }
}
