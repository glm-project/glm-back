package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;

public record Engagement(Auteur auteur, Instant date) {
  public Engagement {
    Assert.notNull("auteur", auteur);
    Assert.notNull("date d'engagement", date);
  }
}
