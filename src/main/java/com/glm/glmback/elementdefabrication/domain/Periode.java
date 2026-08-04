package com.glm.glmback.elementdefabrication.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;

public record Periode(Instant debut, Instant fin) {
  public Periode {
    Assert.notNull("debut", debut);
    Assert.field("fin", fin).afterOrAt(debut);
  }

  public boolean contains(Instant date) {
    return !date.isBefore(debut) && !date.isAfter(fin);
  }
}
