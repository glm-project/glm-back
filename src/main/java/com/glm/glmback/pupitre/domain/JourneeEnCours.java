package com.glm.glmback.pupitre.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.Optional;

/**
 * La journee sans depart la plus recente d'un operateur, telle que la base la projette : son etat et son arrivee.
 *
 * <p>
 * Elle ne dit pas encore si l'operateur est present : une journee dont l'amplitude depasse le seuil est abandonnee,
 * et l'operateur redevient absent. C'est l'instant de lecture qui en decide.
 * </p>
 */
public record JourneeEnCours(EtatDePresence etat, Instant arrivee) {
  public JourneeEnCours {
    Assert.notNull("etat de presence", etat);
    Assert.notNull("arrivee", arrivee);
  }

  /**
   * Presente jusqu'a l'arrivee plus le seuil, bornes comprises ; absente au-dela.
   */
  public PresenceDuPupitre a(Instant maintenant, AmplitudeMaximale seuil) {
    Instant echeance = arrivee.plus(seuil.value());

    if (maintenant.isAfter(echeance)) {
      return PresenceDuPupitre.absente();
    }

    return new PresenceDuPupitre(etat, Optional.of(echeance));
  }
}
