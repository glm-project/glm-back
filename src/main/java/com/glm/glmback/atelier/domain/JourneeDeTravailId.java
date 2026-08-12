package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.UUID;

public record JourneeDeTravailId(UUID uuid) implements Comparable<JourneeDeTravailId> {
  public JourneeDeTravailId {
    Assert.notNull("id de la journee de travail", uuid);
  }

  public static JourneeDeTravailId newId() {
    return new JourneeDeTravailId(UUID.randomUUID());
  }

  @Override
  public int compareTo(JourneeDeTravailId other) {
    return uuid().compareTo(other.uuid());
  }
}
