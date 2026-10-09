package com.glm.glmback.naturedetravail.domain;

import com.glm.glmback.shared.error.domain.Assert;

/**
 * Une nature telle que la liste la montre : avec, en plus, le fait qu'elle soit deja utilisee, ce qui interdit de la
 * supprimer, et le nombre de postes qui la portent.
 *
 * <p>
 * Les deux ne se deduisent pas l'un de l'autre : une nature sans poste reste utilisee des que du temps a ete pointe
 * sous elle.
 * </p>
 */
public record NatureDeTravailListee(NatureDeTravail nature, boolean utilisee, int postes) {
  public NatureDeTravailListee {
    Assert.notNull("nature", nature);
    Assert.field("postes", postes).min(0);
  }
}
