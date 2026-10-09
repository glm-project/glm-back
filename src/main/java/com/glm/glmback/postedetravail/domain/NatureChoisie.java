package com.glm.glmback.postedetravail.domain;

import com.glm.glmback.shared.error.domain.Assert;

/**
 * La nature qu'un gestionnaire donne a un poste : designee par son identifiant dans le referentiel, ou, le temps de la
 * transition du front, par son libelle saisi en texte.
 */
public sealed interface NatureChoisie {
  record ParIdentifiant(NatureDeTravailId id) implements NatureChoisie {
    public ParIdentifiant {
      Assert.notNull("id de la nature de travail", id);
    }
  }

  /**
   * @deprecated chemin de transition, retire quand le front envoie l'identifiant (glm-back#130).
   */
  @Deprecated(forRemoval = true)
  record ParLibelle(NatureDeTravail libelle) implements NatureChoisie {
    public ParLibelle {
      Assert.notNull("nature de travail", libelle);
    }
  }
}
