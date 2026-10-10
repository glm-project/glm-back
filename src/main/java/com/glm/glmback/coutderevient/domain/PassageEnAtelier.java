package com.glm.glmback.coutderevient.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.Optional;

/** Un passage de l'element en atelier, avec l'instant de sa cloture s'il est clos. */
public record PassageEnAtelier(Optional<Instant> cloture) {
  public PassageEnAtelier {
    Assert.notNull("cloture", cloture);
  }
}
