package com.glm.glmback.syntheseheures.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.LocalDate;

/** Une portion d'activite rattachee a son jour calendaire. */
public record IntervalleDUnJour(LocalDate jour, IntervalleDActivite intervalle) {
  public IntervalleDUnJour {
    Assert.notNull("jour", jour);
    Assert.notNull("intervalle", intervalle);
  }
}
