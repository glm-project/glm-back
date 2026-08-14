package com.glm.glmback.feuilledetemps.domain;

import com.glm.glmback.shared.error.domain.Assert;

/**
 * L'identite d'un operateur, relue au referentiel a chaque lecture.
 *
 * <p>
 * Rien n'en est fige : une faute de frappe corrigee sur une fiche doit s'afficher corrigee sur tout l'historique, y
 * compris les semaines deja passees. Ni taux horaire ni habilitation ici — ce contexte affiche du temps, il ne le
 * valorise pas.
 * </p>
 */
public record OperateurConnu(OperateurId id, Nom nom, Prenom prenom) {
  public OperateurConnu {
    Assert.notNull("id de l'operateur", id);
    Assert.notNull("nom", nom);
    Assert.notNull("prenom", prenom);
  }
}
