package com.glm.glmback.atelier.application.gestionanomalies;

import com.glm.glmback.shared.error.domain.Assert;

public record ContexteDeResolution(String tenant, IdentiteDuGestionnaire gestionnaire) {
  public ContexteDeResolution {
    Assert.notBlank("tenant", tenant);
    Assert.notNull("gestionnaire", gestionnaire);
  }

  public boolean correspondA(ContexteDeResolution autre) {
    return tenant.equals(autre.tenant) && gestionnaire.estLaMemePersonneQue(autre.gestionnaire);
  }
}
