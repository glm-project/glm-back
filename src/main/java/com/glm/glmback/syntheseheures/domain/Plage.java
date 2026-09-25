package com.glm.glmback.syntheseheures.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.Optional;

/**
 * Un intervalle de temps, eventuellement encore ouvert.
 *
 * <p>
 * C'est la matiere premiere de la synthese des heures : une fenetre de presence, une tranche de travail, et ce qu'il
 * en reste une fois coupe a minuit sont tous des plages. Une plage sans fin n'est pas une anomalie — l'operateur
 * n'est simplement pas encore parti.
 * </p>
 */
public record Plage(Instant debut, Optional<Instant> fin, boolean presumee) {
  public Plage {
    Assert.notNull("debut", debut);
    Assert.notNull("fin", fin);
    fin.ifPresent(date -> Assert.field("fin", date).afterOrAt(debut));
  }

  /**
   * Une plage pointee : ses bornes sont des faits de presence. Seule la fin presumee d'une journee abandonnee en
   * produit une presumee.
   */
  public Plage(Instant debut, Optional<Instant> fin) {
    this(debut, fin, false);
  }

  /**
   * Vrai si l'instant tombe dans la plage, bornes comprises. Une plage ouverte n'a pas de borne haute.
   */
  public boolean contient(Instant instant) {
    return !instant.isBefore(debut) && fin.filter(instant::isAfter).isEmpty();
  }

  public boolean estOuverte() {
    return fin.isEmpty();
  }
}
