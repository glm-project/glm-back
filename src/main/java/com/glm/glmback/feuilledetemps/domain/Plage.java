package com.glm.glmback.feuilledetemps.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.Optional;

/**
 * Les bornes d'une activite entiere ou d'une portion calendaire, eventuellement sans fin.
 */
public record Plage(Instant debut, Optional<Instant> fin) {
  public Plage {
    Assert.notNull("debut", debut);
    Assert.notNull("fin", fin);
    fin.ifPresent(date -> Assert.field("fin", date).afterOrAt(debut));
  }

  public boolean estOuverte() {
    return fin.isEmpty();
  }
}
