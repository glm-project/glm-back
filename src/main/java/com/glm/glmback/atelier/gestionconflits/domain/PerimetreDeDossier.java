package com.glm.glmback.atelier.gestionconflits.domain;

import com.glm.glmback.atelier.domain.ActiviteId;
import com.glm.glmback.atelier.domain.CleDActivite;
import com.glm.glmback.atelier.domain.EvenementDAtelierId;
import com.glm.glmback.shared.error.domain.Assert;
import java.util.List;

/** Les identites de faits et d'activites du dossier, avec la cle de son adresse. */
public record PerimetreDeDossier(CleDActivite cle, List<ActiviteId> activites, List<EvenementDAtelierId> pointages) {
  public PerimetreDeDossier {
    Assert.notNull("cle du dossier", cle);
    Assert.field("activites du dossier", activites).notNull().noNullElement();
    Assert.field("pointages du dossier", pointages).notEmpty().noNullElement();
    activites = List.copyOf(activites);
    pointages = List.copyOf(pointages);
  }
}
