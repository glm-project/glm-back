package com.glm.glmback.atelier.gestionconflits.domain;

import com.glm.glmback.atelier.domain.EvenementDAtelierId;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import com.glm.glmback.shared.error.domain.Assert;

public record AdresseDossierConflit(SuiviDAtelierId suivi, EvenementDAtelierId pointage) {
  public AdresseDossierConflit {
    Assert.notNull("suivi", suivi);
    Assert.notNull("pointage", pointage);
  }
}
