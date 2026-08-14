package com.glm.glmback.feuilledetemps.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.Optional;

/**
 * Un intervalle de temps, eventuellement encore ouvert.
 *
 * <p>
 * C'est la matiere premiere de la feuille de temps : une fenetre de presence, une tranche de travail, et ce qu'il en
 * reste une fois coupe a minuit sont tous des plages. Une plage sans fin n'est pas une anomalie — l'operateur n'est
 * simplement pas encore parti.
 * </p>
 */
public record Plage(Instant debut, Optional<Instant> fin) {
  public Plage {
    Assert.notNull("debut", debut);
    Assert.notNull("fin", fin);
    fin.ifPresent(date -> Assert.field("fin", date).afterOrAt(debut));
  }

  public boolean estOuverte() {
    return fin.isEmpty();
  }
}
