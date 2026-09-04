package com.glm.glmback.atelier.application;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Le contenu stable d'un geste du pupitre, independant de l'etat courant des agregats. */
public record EmpreinteDEvenement(
  NatureDeGesteDuPupitre nature,
  Optional<UUID> cible,
  UUID operateur,
  String type,
  Optional<UUID> poste,
  Optional<Instant> dateDeSurvenue
) {
  public EmpreinteDEvenement {
    Assert.notNull("nature", nature);
    Assert.notNull("cible", cible);
    Assert.notNull("operateur", operateur);
    Assert.notBlank("type", type);
    Assert.notNull("poste", poste);
    Assert.notNull("dateDeSurvenue", dateDeSurvenue);
  }
}
