package com.glm.glmback.syntheseheures.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.Optional;

/**
 * Une activite et la plage pendant laquelle elle a couru, telle que le repli du journal la produit.
 *
 * <p>
 * La plage peut rester ouverte : un travail en cours n'a pas encore de fin, et la synthese des heures ne l'invente pas.
 * </p>
 */
public record IntervalleDActivite(Activite activite, Plage plage) {
  public IntervalleDActivite {
    Assert.notNull("activite", activite);
    Assert.notNull("plage", plage);
  }

  /**
   * Le meme intervalle reduit a la fenetre de presence donnee, s'il en reste quelque chose. Un depart referme ce que
   * l'operateur a oublie d'arreter.
   */
  public Optional<IntervalleDActivite> reduitA(Plage fenetre) {
    return plage.intersection(fenetre).map(this::sur);
  }

  IntervalleDActivite sur(Plage autre) {
    return new IntervalleDActivite(activite, autre);
  }
}
