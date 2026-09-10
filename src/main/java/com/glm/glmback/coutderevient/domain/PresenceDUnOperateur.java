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
   * La venue pendant laquelle cet instant tombe, s'il y en a une.
   */
  public Optional<JourneeDeTravail> journeeContenant(Instant instant) {
    return journees
      .stream()
      .filter(journee -> journee.contient(instant))
      .findFirst();
  }
}
