package com.glm.glmback.naturedetravail.domain;

import com.glm.glmback.shared.error.domain.Assert;

/**
 * Une nature telle que la liste la montre : avec, en plus, le fait qu'elle soit deja utilisee, ce qui interdit de la
 * supprimer.
 */
public record NatureDeTravailListee(NatureDeTravail nature, boolean utilisee) {
  public NatureDeTravailListee {
    Assert.notNull("nature", nature);
  }
}
