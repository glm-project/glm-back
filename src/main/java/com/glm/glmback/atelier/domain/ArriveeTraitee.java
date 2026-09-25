package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;

/**
 * Ce qu'une arrivee a produit : la journee qui l'a recue, et si elle y a ete absorbee.
 *
 * <p>
 * Une arrivee redondante, sous le seuil, est absorbee : l'operateur est deja la, rien n'est ajoute au journal.
 * C'est au domaine d'en decider, et a l'application de le traduire comme un rejeu.
 * </p>
 */
public record ArriveeTraitee(JourneeDeTravail journee, boolean absorbee) {
  public ArriveeTraitee {
    Assert.notNull("journee", journee);
  }
}
