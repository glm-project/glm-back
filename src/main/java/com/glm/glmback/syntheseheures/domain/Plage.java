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

  /**
   * La part commune a cette plage et a la fenetre, s'il en reste une de duree non nulle. Deux plages encore ouvertes
   * se croisent en une plage encore ouverte, et ce qu'une fenetre presumee borne devient presume.
   */
  public Optional<Plage> intersection(Plage fenetre) {
    Instant debutCommun = debut.isAfter(fenetre.debut) ? debut : fenetre.debut;
    Optional<Instant> finCommune = plusTot(fin, fenetre.fin);

    if (finCommune.filter(date -> !date.isAfter(debutCommun)).isPresent()) {
      return Optional.empty();
    }

    return Optional.of(new Plage(debutCommun, finCommune, presumee || fenetre.presumee));
  }

  private static Optional<Instant> plusTot(Optional<Instant> une, Optional<Instant> autre) {
    return une.map(date -> autre.filter(date::isAfter).orElse(date)).or(() -> autre);
  }
}
