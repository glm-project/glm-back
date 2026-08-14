package com.glm.glmback.feuilledetemps.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.LocalDate;

/**
 * Une plage rattachee au jour calendaire dans lequel elle tombe.
 *
 * <p>
 * C'est ici que le calendrier entre dans le modele, et nulle part avant : l'atelier ne connait que des instants, et
 * c'est le fuseau horaire de l'entreprise qui decide a quel jour appartient une heure donnee.
 * </p>
 */
public record PlageDUnJour(LocalDate jour, Plage plage) {
  public PlageDUnJour {
    Assert.notNull("jour", jour);
    Assert.notNull("plage", plage);
  }
}
