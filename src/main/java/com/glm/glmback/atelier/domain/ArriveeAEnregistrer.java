package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.Optional;

/**
 * L'arrivee ouvre la journee de travail.
 *
 * <p>
 * Une date de survenue absente vaut maintenant : l'operateur qui s'identifie le matin et le gestionnaire qui saisit a
 * posteriori une arrivee jamais pointee passent par le meme acte, seul l'instant change.
 * </p>
 */
public record ArriveeAEnregistrer(OperateurId operateur, Auteur auteur, Optional<Instant> dateDeSurvenue, EvenementDePresenceId evenement) {
  public ArriveeAEnregistrer {
    Assert.notNull("operateur", operateur);
    Assert.notNull("auteur", auteur);
    Assert.notNull("dateDeSurvenue", dateDeSurvenue);
    Assert.notNull("id de l'evenement", evenement);
  }

  public ArriveeAEnregistrer(OperateurId operateur, Auteur auteur) {
    this(operateur, auteur, Optional.empty(), EvenementDePresenceId.newId());
  }

  public ArriveeAEnregistrer(OperateurId operateur, Auteur auteur, Optional<Instant> dateDeSurvenue) {
    this(operateur, auteur, dateDeSurvenue, EvenementDePresenceId.newId());
  }

  public TypeDEvenementDePresence type() {
    return TypeDEvenementDePresence.ARRIVEE;
  }
}
