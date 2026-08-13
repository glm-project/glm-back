package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.UUID;

/**
 * L'identite de ce que l'operateur engage en pointant : une machine, un etabli, un four, une salle.
 *
 * <p>
 * Le terme reste volontairement generique. L'application s'adresse a plusieurs entreprises clientes, dont certaines
 * n'ont aucun parc machine : le poste est partout facultatif, et son absence redonne exactement le comportement d'un
 * atelier ou l'operateur est la seule ressource.
 * </p>
 */
public record PosteDeTravailId(UUID uuid) {
  public PosteDeTravailId {
    Assert.notNull("id du poste de travail", uuid);
  }
}
