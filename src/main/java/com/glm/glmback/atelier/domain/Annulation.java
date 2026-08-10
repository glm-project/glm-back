package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;

public record Annulation(Auteur auteur, Instant date, MotifDAnnulation motif) {
  public Annulation {
    Assert.notNull("auteur", auteur);
    Assert.notNull("date", date);
    Assert.notNull("motif", motif);
  }
}
