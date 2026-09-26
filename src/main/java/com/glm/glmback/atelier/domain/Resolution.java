package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;

public record Resolution(TypeDeResolution type, Auteur auteur, Instant date) {
  public Resolution {
    Assert.notNull("type de resolution", type);
    Assert.notNull("auteur", auteur);
    Assert.notNull("date de resolution", date);
  }
}
