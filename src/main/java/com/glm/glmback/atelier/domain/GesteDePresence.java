package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.Optional;

public record GesteDePresence(
  OperateurId operateur,
  TypeDEvenementDePresence type,
  Optional<Instant> dateDeclaree
) implements GesteEnAttente {
  public GesteDePresence {
    Assert.notNull("operateur", operateur);
    Assert.notNull("type", type);
    Assert.notNull("date declaree", dateDeclaree);
  }
}
