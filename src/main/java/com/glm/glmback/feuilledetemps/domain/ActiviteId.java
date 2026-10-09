package com.glm.glmback.feuilledetemps.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.UUID;

/** Identite stable de l'activite : l'identifiant de son pointage ouvrant d'origine. */
public record ActiviteId(UUID uuid) {
  public ActiviteId {
    Assert.notNull("id de l'activite", uuid);
  }
}
