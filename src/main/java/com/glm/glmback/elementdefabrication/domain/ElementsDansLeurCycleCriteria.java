package com.glm.glmback.elementdefabrication.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.Optional;

/**
 * Les elements a lire, et l'instant auquel l'atelier juge l'etat de ceux qui y sont.
 */
public record ElementsDansLeurCycleCriteria(FiltreDesElements filtre, Optional<CycleDeVie> cycle, Instant evaluation) {
  public ElementsDansLeurCycleCriteria {
    Assert.notNull("filtre", filtre);
    Assert.notNull("cycle", cycle);
    Assert.notNull("evaluation", evaluation);
  }

  public boolean matches(ElementDansSonCycle lu) {
    return filtre.matches(lu.element()) && cycle.map(demande -> demande == lu.cycle()).orElse(true);
  }
}
