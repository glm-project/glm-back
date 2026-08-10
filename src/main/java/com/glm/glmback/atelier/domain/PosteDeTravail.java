package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;

/**
 * Ce que l'operateur engage en pointant : une machine, un etabli, un four, une salle.
 *
 * <p>
 * Le terme est volontairement generique. L'application s'adresse a plusieurs entreprises clientes, dont certaines
 * n'ont aucun parc machine : le poste est partout facultatif, et son absence redonne exactement le comportement d'un
 * atelier ou l'operateur est la seule ressource.
 * </p>
 */
public record PosteDeTravail(String value) {
  private static final int MAX_LENGTH = 100;

  public PosteDeTravail {
    Assert.field("poste de travail", value).notBlank().maxLength(MAX_LENGTH);
  }
}
