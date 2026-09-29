package com.glm.glmback.feuilledetemps.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.LocalDate;

/**
 * Une portion d'activite rattachee au jour calendaire dans lequel elle tombe.
 */
public record IntervalleDUnJour(LocalDate jour, IntervalleDActivite intervalle) {
  public IntervalleDUnJour {
    Assert.notNull("jour", jour);
    Assert.notNull("intervalle", intervalle);
  }
}
