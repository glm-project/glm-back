package com.glm.glmback.parametrage.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;

public record Modification(Auteur auteur, Instant date) {
  public Modification {
    Assert.notNull("auteur", auteur);
    Assert.notNull("date de modification", date);
  }
}
