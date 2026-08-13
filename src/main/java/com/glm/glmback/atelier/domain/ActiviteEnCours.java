package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.Optional;

/**
 * Ce que l'ecran d'atelier affiche : qui fait quoi, dans quel etat, depuis quand.
 */
public record ActiviteEnCours(CleDActivite activite, CategorieDActivite categorie, Instant depuis) {
  public ActiviteEnCours {
    Assert.notNull("activite", activite);
    Assert.notNull("categorie", categorie);
    Assert.notNull("depuis", depuis);
  }

  static ActiviteEnCours of(IntervalleDActivite intervalle) {
    return new ActiviteEnCours(intervalle.cle(), intervalle.categorie(), intervalle.debut());
  }

  public OperateurId operateur() {
    return activite.operateur();
  }

  public Optional<PosteDeTravailId> poste() {
    return activite.poste();
  }
}
