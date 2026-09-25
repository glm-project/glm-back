package com.glm.glmback.coutderevient.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Les venues d'un operateur sur la periode lue.
 */
public record PresenceDUnOperateur(OperateurId operateur, List<JourneeDeTravail> journees) {
  public PresenceDUnOperateur {
    Assert.notNull("operateur", operateur);
    Assert.field("journees", journees).notNull().noNullElement();
  }

  /**
   * Les memes venues lues a cet instant : celles qui sont abandonnees recoivent leur fin presumee, a partir des
   * pointages d'OF de l'operateur.
   */
  public PresenceDUnOperateur presumee(Instant maintenant, AmplitudeMaximale seuil, List<Instant> pointages) {
    return new PresenceDUnOperateur(
      operateur,
      journees
        .stream()
        .map(journee -> journee.presumee(maintenant, seuil, pointages))
        .toList()
    );
  }

  /**
   * La venue pendant laquelle cet instant tombe, s'il y en a une.
   */
  public Optional<JourneeDeTravail> journeeContenant(Instant instant) {
    return journees
      .stream()
      .filter(journee -> journee.contient(instant))
      .findFirst();
  }
}
