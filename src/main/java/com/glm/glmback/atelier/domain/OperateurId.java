package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.UUID;

/**
 * L'identite de la personne dont le temps est affecte, telle que le referentiel des operateurs la designe.
 *
 * <p>
 * Le journal ne retient que cet identifiant : contrairement au nom de l'element, copie a l'engagement, aucun libelle
 * n'est fige ici. Une faute de frappe corrigee sur une fiche d'operateur doit s'afficher corrigee sur toutes les
 * feuilles de temps, y compris les anciennes.
 * </p>
 */
public record OperateurId(UUID uuid) {
  public OperateurId {
    Assert.notNull("id de l'operateur", uuid);
  }
}
