package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;

/**
 * Une correction de presence est une annulation et une regularisation, jouees en un seul acte.
 */
public record CorrectionDePresenceAEnregistrer(
  EvenementDePresenceId evenement,
  MotifDAnnulation motif,
  RegularisationDePresenceAEnregistrer remplacement
) {
  public CorrectionDePresenceAEnregistrer {
    Assert.notNull("evenement", evenement);
    Assert.notNull("motif", motif);
    Assert.notNull("remplacement", remplacement);
  }
}
