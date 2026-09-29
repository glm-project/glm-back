package com.glm.glmback.feuilledetemps.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.LocalDate;
import java.util.List;

/**
 * Un jour du calendrier et les portions d'activite qui lui reviennent, vide compris.
 */
public record JourDeLaSemaine(LocalDate jour, List<IntervalleDActivite> activites) {
  public JourDeLaSemaine {
    Assert.notNull("jour", jour);
    Assert.field("activites", activites).notNull().noNullElement();
  }
}
