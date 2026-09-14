package com.glm.glmback.pupitre.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.Optional;

/**
 * Ce que la tuile du pupitre affiche : qui travaille sur cet element, dans quel etat, depuis quand.
 */
public record ActiviteEnCours(CleDActivite activite, CategorieDActivite categorie, Instant depuis) {
  public ActiviteEnCours {
    Assert.notNull("activite", activite);
    Assert.notNull("categorie", categorie);
    Assert.notNull("depuis", depuis);
  }

  public OperateurId operateur() {
    return activite.operateur();
  }

  public Optional<PosteDeTravailId> poste() {
    return activite.poste();
  }
}
