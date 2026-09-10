package com.glm.glmback.syntheseheures.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

/**
 * Un jour du calendrier, ses pointages et le temps travaille qui lui revient.
 *
 * <p>
 * Les sept jours sont toujours rendus, meme vides : un trou dans la liste obligerait le lecteur a deviner s'il
 * manque une journee ou si l'operateur n'etait pas la.
 * </p>
 */
public record JourDeSynthese(LocalDate jour, List<EvenementDePresence> pointages, Duration duree) {
  public JourDeSynthese {
    Assert.notNull("jour", jour);
    Assert.field("pointages", pointages).notNull().noNullElement();
    Assert.notNull("duree", duree);
  }
}
