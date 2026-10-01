package com.glm.glmback.coutderevient.domain;

import com.glm.glmback.shared.error.domain.Assert;

/** Toute la plage possible d'une activite a resoudre, bornee par evaluation. */
public record ZoneIncertaine(ActiviteInterpretee activite, Periode periode) {
  public ZoneIncertaine {
    Assert.notNull("activite", activite);
    Assert.notNull("periode", periode);
  }
}
