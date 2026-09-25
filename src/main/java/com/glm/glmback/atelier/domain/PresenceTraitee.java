package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;

/**
 * Ce qu'un geste de presence a produit : la journee qui l'a recu, et s'il y a ete absorbe.
 *
 * <p>
 * Une arrivee redondante, sous le seuil, est absorbee : l'operateur est deja la, rien n'est ajoute au journal.
 * C'est au domaine d'en decider, et a l'application de le traduire comme un rejeu.
 * </p>
 */
public record PresenceTraitee(JourneeDeTravail journee, boolean absorbee) {
  public PresenceTraitee {
    Assert.notNull("journee", journee);
  }
}
