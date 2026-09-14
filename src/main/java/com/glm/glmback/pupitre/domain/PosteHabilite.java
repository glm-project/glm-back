package com.glm.glmback.pupitre.domain;

import com.glm.glmback.shared.error.domain.Assert;

/**
 * Un poste sur lequel l'operateur est declare, tel que le pupitre le propose.
 *
 * <p>
 * Ni nature ni cout horaire : le pupitre n'offre ce poste qu'au choix, il ne valorise rien. C'est cette liste qui
 * evite de laisser le serveur refuser un pointage non habilite par un 409.
 * </p>
 */
public record PosteHabilite(PosteDeTravailId id, LibelleDePoste libelle) {
  public PosteHabilite {
    Assert.notNull("id du poste de travail", id);
    Assert.notNull("libelle du poste", libelle);
  }
}
