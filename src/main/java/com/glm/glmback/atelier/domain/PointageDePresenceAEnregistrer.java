package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.Optional;

/**
 * Un depart, saisi en direct sur la journee en cours de l'operateur.
 *
 * <p>
 * Un seul evenement, quel que soit le nombre d'elements sur lesquels l'operateur travaille : c'est ce qui donne au
 * client son bouton d'arret de fin de journee, sans jamais N clics pour N taches.
 * </p>
 */
public record PointageDePresenceAEnregistrer(
  OperateurId operateur,
  Auteur auteur,
  TypeDEvenementDePresence type,
  Optional<Instant> dateDeSurvenue,
  EvenementDePresenceId evenement
) {
  public PointageDePresenceAEnregistrer {
    Assert.notNull("operateur", operateur);
    Assert.notNull("auteur", auteur);
    Assert.notNull("type", type);
    Assert.notNull("dateDeSurvenue", dateDeSurvenue);
    Assert.notNull("id de l'evenement", evenement);
  }

  public PointageDePresenceAEnregistrer(OperateurId operateur, Auteur auteur, TypeDEvenementDePresence type) {
    this(operateur, auteur, type, Optional.empty(), EvenementDePresenceId.newId());
  }
}
