package com.glm.glmback.atelier.gestionconflits.domain;

import com.glm.glmback.atelier.domain.ActiviteId;
import com.glm.glmback.atelier.domain.EvenementDAtelierId;
import com.glm.glmback.shared.error.domain.Assert;
import java.util.Optional;

/** Une hypothese issue du diagnostic ; aucun motif ni decision n'est choisi pour le gestionnaire. */
public record PropositionDeResolution(CodeDeProposition code, EvenementDAtelierId pointage, Optional<ActiviteId> activiteVisee) {
  public PropositionDeResolution {
    Assert.notNull("code de proposition", code);
    Assert.notNull("pointage", pointage);
    Assert.notNull("activite visee", activiteVisee);
  }
}
