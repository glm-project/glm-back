package com.glm.glmback.coutderevient.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.Optional;

/** Bornes de l'activite : seule une fin interpretable permet de la valoriser. */
public record Plage(Instant debut, Optional<Instant> fin) {
  public Plage {
    Assert.notNull("debut", debut);
    Assert.notNull("fin", fin);
    fin.ifPresent(date -> Assert.field("fin", date).afterOrAt(debut));
  }
}
