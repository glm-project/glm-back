package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;

public record Ecart(Auteur auteur, Instant date, MotifDEcart motif) implements TraitementDuPointage {
  public Ecart {
    Assert.notNull("auteur", auteur);
    Assert.notNull("date", date);
    Assert.notNull("motif", motif);
  }
}
