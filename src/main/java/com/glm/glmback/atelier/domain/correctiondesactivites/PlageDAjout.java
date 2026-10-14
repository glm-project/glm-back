package com.glm.glmback.atelier.domain.correctiondesactivites;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;

/**
 * Le debut et la fin d'une activite que le gestionnaire ajoute. La fin peut tomber le lendemain du debut. Le domaine ne
 * la verifie qu'a l'ajout, une fois les ressources resolues : un refus de plage vient apres les refus de ressource.
 */
public record PlageDAjout(Instant debut, Instant fin) {
  public PlageDAjout {
    Assert.notNull("debut", debut);
    Assert.notNull("fin", fin);
  }
}
