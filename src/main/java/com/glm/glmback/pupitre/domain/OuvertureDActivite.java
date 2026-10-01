package com.glm.glmback.pupitre.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.UUID;

/** L'identite stable et les bornes publiees par l'atelier pour une activite. */
public record OuvertureDActivite(UUID id, Instant depuis, Instant echeance) {
  public OuvertureDActivite {
    Assert.notNull("ouverture", id);
    Assert.notNull("depuis", depuis);
    Assert.notNull("echeance", echeance);
  }
}
