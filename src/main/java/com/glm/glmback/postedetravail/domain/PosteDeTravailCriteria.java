package com.glm.glmback.postedetravail.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.Optional;

/**
 * Regles de selection du referentiel des postes.
 *
 * <p>
 * La correspondance vit ici, dans le domaine, pour que le double en memoire et l'adapter de persistance ne puissent pas
 * diverger. Une nature absente ne filtre rien.
 * </p>
 */
public record PosteDeTravailCriteria(Optional<NatureDeTravail> nature) {
  public PosteDeTravailCriteria {
    Assert.notNull("nature de travail", nature);
  }

  public boolean matches(PosteDeTravail poste) {
    return nature.map(attendue -> attendue.equals(poste.nature())).orElse(true);
  }
}
