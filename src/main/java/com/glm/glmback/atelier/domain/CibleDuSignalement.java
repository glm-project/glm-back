package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.UUID;

/**
 * La journee ou le suivi d'atelier dont le journal porte le pointage signale : c'est la que le gestionnaire le
 * corrige ou l'annule.
 */
public record CibleDuSignalement(TypeDeCible type, UUID id) {
  public CibleDuSignalement {
    Assert.notNull("type de cible", type);
    Assert.notNull("id de la cible", id);
  }
}
