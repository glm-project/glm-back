package com.glm.glmback.coutderevient.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.UUID;

/**
 * L'identite de ce que l'operateur engage en pointant.
 *
 * <p>
 * C'est l'unite du diviseur : le taux horaire d'un operateur se divise par le nombre de postes qu'il occupe de front,
 * jamais par le nombre d'elements sur lesquels il travaille.
 * </p>
 */
public record PosteDeTravailId(UUID uuid) {
  public PosteDeTravailId {
    Assert.notNull("id du poste de travail", uuid);
  }
}
