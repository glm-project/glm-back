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
public record ArriveeAEnregistrer(Operateur operateur, Auteur auteur, Optional<Instant> dateDeSurvenue) {
  public ArriveeAEnregistrer {
    Assert.notNull("operateur", operateur);
    Assert.notNull("auteur", auteur);
    Assert.notNull("dateDeSurvenue", dateDeSurvenue);
  }

  public ArriveeAEnregistrer(Operateur operateur, Auteur auteur) {
    this(operateur, auteur, Optional.empty());
  }
}
