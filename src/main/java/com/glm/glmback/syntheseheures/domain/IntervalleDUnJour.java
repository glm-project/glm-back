package com.glm.glmback.syntheseheures.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.LocalDate;

/**
 * Un intervalle d'activite rattache au jour calendaire dans lequel il tombe, comme {@link PlageDUnJour} l'est pour la
 * presence.
 */
public record IntervalleDUnJour(LocalDate jour, IntervalleDActivite intervalle) {
  public IntervalleDUnJour {
    Assert.notNull("jour", jour);
    Assert.notNull("intervalle", intervalle);
  }
}
