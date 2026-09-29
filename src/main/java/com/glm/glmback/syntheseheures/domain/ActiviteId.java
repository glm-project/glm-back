package com.glm.glmback.syntheseheures.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.UUID;

/** Identite stable de l'activite, conservee quand son ouvrant est corrige. */
public record ActiviteId(UUID uuid) {
  public ActiviteId {
    Assert.notNull("id de l'activite", uuid);
  }
}
