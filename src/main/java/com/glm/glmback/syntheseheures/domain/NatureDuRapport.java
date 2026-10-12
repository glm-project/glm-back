package com.glm.glmback.syntheseheures.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.Optional;

/** Le temps pointe sur une nature de travail ; l'absence de nature, pointee sans poste, est un cas nominal. */
public record NatureDuRapport(Optional<NatureDOperation> nature, DureeTotale duree) {
  public NatureDuRapport {
    Assert.notNull("nature de l'operation", nature);
    Assert.notNull("duree", duree);
  }
}
