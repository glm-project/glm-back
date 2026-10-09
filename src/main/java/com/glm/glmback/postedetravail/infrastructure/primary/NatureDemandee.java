package com.glm.glmback.postedetravail.infrastructure.primary;

import com.glm.glmback.postedetravail.domain.NatureChoisie;
import com.glm.glmback.postedetravail.domain.NatureDeTravail;
import com.glm.glmback.postedetravail.domain.NatureDeTravailId;
import java.util.UUID;

/**
 * La nature d'un poste telle qu'une requete la designe : par son identifiant, ou, le temps de la transition du front,
 * par son libelle. L'identifiant l'emporte quand les deux sont donnes.
 */
final class NatureDemandee {

  private NatureDemandee() {}

  static boolean estDesignee(UUID natureId, String nature) {
    return natureId != null || (nature != null && !nature.isBlank());
  }

  @SuppressWarnings("removal")
  static NatureChoisie choisie(UUID natureId, String nature) {
    if (natureId != null) {
      return new NatureChoisie.ParIdentifiant(new NatureDeTravailId(natureId));
    }

    return new NatureChoisie.ParLibelle(new NatureDeTravail(nature));
  }
}
