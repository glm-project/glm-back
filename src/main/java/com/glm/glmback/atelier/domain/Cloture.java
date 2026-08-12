package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;

public record Cloture(Auteur auteur, Horodatage horodatage) {
  public Cloture {
    Assert.notNull("auteur", auteur);
    Assert.notNull("horodatage", horodatage);
  }

  public Instant dateDeSurvenue() {
    return horodatage.dateDeSurvenue();
  }
}
