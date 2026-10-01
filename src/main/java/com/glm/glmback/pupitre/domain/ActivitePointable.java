package com.glm.glmback.pupitre.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.Optional;

/** L'activite interpretable que le pupitre peut viser par une fin ou une transition. */
public record ActivitePointable(CleDActivite activite, CategorieDActivite categorie, OuvertureDActivite ouverture) {
  public ActivitePointable {
    Assert.notNull("activite", activite);
    Assert.notNull("categorie", categorie);
    Assert.notNull("ouverture", ouverture);
  }

  public OperateurId operateur() {
    return activite.operateur();
  }

  public Optional<PosteDeTravailId> poste() {
    return activite.poste();
  }

  public Instant depuis() {
    return ouverture.depuis();
  }

  public Instant echeance() {
    return ouverture.echeance();
  }
}
