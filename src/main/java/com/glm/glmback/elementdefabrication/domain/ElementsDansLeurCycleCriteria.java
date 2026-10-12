package com.glm.glmback.elementdefabrication.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.Optional;

/**
 * Les elements a lire, et l'instant auquel l'atelier juge l'etat de ceux qui y sont.
 */
public record ElementsDansLeurCycleCriteria(Optional<Periode> creation, Instant evaluation) {
  public ElementsDansLeurCycleCriteria {
    Assert.notNull("creation", creation);
    Assert.notNull("evaluation", evaluation);
  }

  public boolean matches(ElementDansSonCycle lu) {
    return creation.map(periode -> periode.contains(lu.element().dateDeCreation())).orElse(true);
  }
}
