package com.glm.glmback.pupitre.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.Optional;

/**
 * Ce que le pupitre affiche de la presence d'un operateur : son etat, et l'instant jusqu'auquel il reste present sans
 * nouveau geste. Passe cet instant, sa journee est abandonnee et le pupitre, meme hors ligne, le tient pour absent.
 */
public record PresenceDuPupitre(EtatDePresence etat, Optional<Instant> presentJusqua) {
  public PresenceDuPupitre {
    Assert.notNull("etat de presence", etat);
    Assert.notNull("present jusqu'a", presentJusqua);
  }

  public static PresenceDuPupitre absente() {
    return new PresenceDuPupitre(EtatDePresence.ABSENT, Optional.empty());
  }
}
