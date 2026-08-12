package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;

/**
 * Une correction est une annulation et une regularisation, jouees en un seul acte.
 */
public record CorrectionAEnregistrer(EvenementDAtelierId evenement, MotifDAnnulation motif, RegularisationAEnregistrer remplacement) {
  public CorrectionAEnregistrer {
    Assert.notNull("evenement", evenement);
    Assert.notNull("motif", motif);
    Assert.notNull("remplacement", remplacement);
  }
}
