package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.Optional;

/**
 * Un intervalle pendant lequel un operateur etait present, de son arrivee a son depart.
 *
 * <p>
 * Il decrit la venue de l'operateur, jamais le temps passe sur un element : le temps effectif se lit sur les seules
 * activites, qu'aucune presence ne borne.
 * </p>
 */
public record FenetreDePresence(Instant debut, Optional<Instant> fin) {
  public FenetreDePresence {
    Assert.notNull("debut", debut);
    Assert.notNull("fin", fin);
    fin.ifPresent(date -> Assert.field("fin", date).afterOrAt(debut));
  }

  public boolean estOuverte() {
    return fin.isEmpty();
  }
}
