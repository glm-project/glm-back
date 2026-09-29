package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.Optional;

/**
 * Ce que l'ecran d'atelier affiche : qui fait quoi, dans quel etat, depuis quand.
 */
public record ActiviteEnCours(Activite activite) {
  public ActiviteEnCours {
    Assert.notNull("activite", activite);
  }

  public CleDActivite cle() {
    return activite.cle();
  }

  public OperateurId operateur() {
    return cle().operateur();
  }

  public Optional<PosteDeTravailId> poste() {
    return cle().poste();
  }

  public CategorieDActivite categorie() {
    return activite.categorie();
  }

  public Instant depuis() {
    return activite.debut();
  }
}
