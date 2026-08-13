package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;

/**
 * Ce que l'atelier retient d'un poste du referentiel : son identite, son libelle et la nature du travail qui s'y fait.
 *
 * <p>
 * C'est d'ici que vient la {@link NatureDOperation} recopiee sur l'evenement, et non du profil de la personne : un
 * operateur polyvalent declenche deux pointages, un par poste, et chacun sait de quel metier il releve parce que le
 * poste le dit.
 * </p>
 */
public record PosteConnu(PosteDeTravailId id, LibelleDePoste libelle, NatureDOperation nature) {
  public PosteConnu {
    Assert.notNull("id du poste de travail", id);
    Assert.notNull("libelle du poste", libelle);
    Assert.notNull("nature de l'operation", nature);
  }
}
