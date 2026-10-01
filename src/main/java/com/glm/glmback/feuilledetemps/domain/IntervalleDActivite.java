package com.glm.glmback.feuilledetemps.domain;

import com.glm.glmback.shared.error.domain.Assert;

/**
 * Une activite et la plage pendant laquelle elle a couru, telle que l'atelier la produit.
 *
 * <p>
 * La plage peut rester ouverte : un travail en cours n'a pas encore de fin, et la feuille de temps ne l'invente pas.
 * </p>
 */
public record IntervalleDActivite(Activite activite, Plage plage, ActiviteLue lecture) {
  public IntervalleDActivite {
    Assert.notNull("activite", activite);
    Assert.notNull("plage", plage);
    Assert.notNull("lecture de l'activite", lecture);
  }

  IntervalleDActivite sur(Plage autre) {
    return new IntervalleDActivite(activite, autre, lecture);
  }
}
