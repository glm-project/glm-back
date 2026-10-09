package com.glm.glmback.postedetravail.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.Optional;

/**
 * Regles de selection du referentiel des postes.
 *
 * <p>
 * La correspondance vit ici, dans le domaine, pour que le double en memoire et l'adapter de persistance ne puissent pas
 * diverger. Un critere absent ne filtre rien ; deux criteres donnes se cumulent. La nature se designe par son libelle
 * ou par son identifiant.
 * </p>
 */
public record PosteDeTravailCriteria(Optional<NatureDeTravail> nature, Optional<NatureDeTravailId> natureId) {
  public PosteDeTravailCriteria {
    Assert.notNull("nature de travail", nature);
    Assert.notNull("id de la nature de travail", natureId);
  }

  public static PosteDeTravailCriteria tous() {
    return new PosteDeTravailCriteria(Optional.empty(), Optional.empty());
  }

  public boolean matches(PosteDeTravail poste) {
    return (
      nature.map(attendue -> attendue.equals(poste.nature().libelle())).orElse(true)
      && natureId.map(attendu -> attendu.equals(poste.nature().id())).orElse(true)
    );
  }
}
