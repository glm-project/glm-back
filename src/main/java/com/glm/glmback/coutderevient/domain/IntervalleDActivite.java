package com.glm.glmback.coutderevient.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.Optional;

/**
 * Une activite et la plage pendant laquelle elle a couru, telle que le repli du journal la produit.
 *
 * <p>
 * La plage peut etre encore ouverte : le repli se fait sans horloge, et c'est la lecture qui decide, plus tard,
 * d'arreter le temps a l'instant present.
 * </p>
 */
public record IntervalleDActivite(Activite activite, Plage plage) {
  public IntervalleDActivite {
    Assert.notNull("activite", activite);
    Assert.notNull("plage", plage);
  }

  /**
   * Le meme intervalle reduit a la fenetre de presence donnee, s'il en reste quelque chose.
   *
   * <p>
   * Une pause de midi le scinde en deux, un depart referme ce que l'operateur a oublie d'arreter : aucun de ces deux
   * faits n'a eu besoin d'etre recopie dans le journal de l'element.
   * </p>
   */
  public Optional<IntervalleDActivite> reduitA(Plage fenetre) {
    return plage.intersection(fenetre).map(part -> new IntervalleDActivite(activite, part));
  }

  /**
   * La tranche mesurable que devient cet intervalle si on arrete le temps a l'instant donne.
   */
  public TrancheDActivite ferme(Instant maintenant) {
    return new TrancheDActivite(activite, plage.fermee(maintenant));
  }

  public OperateurId operateur() {
    return activite.operateur();
  }
}
