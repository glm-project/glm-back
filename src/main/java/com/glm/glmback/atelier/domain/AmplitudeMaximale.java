package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Duration;

/**
 * Le seuil au-dela duquel une journee sans depart est abandonnee, lu dans le parametrage de l'entreprise.
 *
 * <p>
 * Ses bornes, a la minute et strictement sous 24 h, sont garanties par le contexte {@code parametrage} et par le
 * schema : l'atelier ne les revalide pas, il les recoit.
 * </p>
 */
public record AmplitudeMaximale(Duration value) {
  public AmplitudeMaximale {
    Assert.notNull("amplitude maximale", value);
  }
}
