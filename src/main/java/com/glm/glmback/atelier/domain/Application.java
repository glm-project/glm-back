package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;

public record Application(Auteur auteur, Instant date) implements TraitementDuPointage {
  public Application {
    Assert.notNull("auteur", auteur);
    Assert.notNull("date", date);
  }
}
