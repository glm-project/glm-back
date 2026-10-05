package com.glm.glmback.atelier.domain.gestionanomalies;

import com.glm.glmback.atelier.domain.EvenementDAtelierId;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import com.glm.glmback.shared.error.domain.Assert;

public record AdresseDossierAnomalie(SuiviDAtelierId suivi, EvenementDAtelierId pointage) {
  public AdresseDossierAnomalie {
    Assert.notNull("suivi", suivi);
    Assert.notNull("pointage", pointage);
  }
}
