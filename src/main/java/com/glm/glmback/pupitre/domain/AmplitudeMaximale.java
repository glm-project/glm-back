package com.glm.glmback.pupitre.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Duration;

/**
 * Le seuil au-dela duquel une journee sans depart est abandonnee, lu dans le parametrage de l'entreprise. Meme
 * notion que dans l'atelier, redeclaree ici : le contexte voisin est {@code BusinessContext} et ne s'importe pas.
 */
public record AmplitudeMaximale(Duration value) {
  public AmplitudeMaximale {
    Assert.notNull("amplitude maximale", value);
  }
}
