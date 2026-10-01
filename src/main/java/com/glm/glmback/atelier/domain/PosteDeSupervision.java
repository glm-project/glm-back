package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.Optional;

public record PosteDeSupervision(PosteDeTravailId id, LibelleDePoste libelle, Optional<NatureDOperation> nature) {
  public PosteDeSupervision {
    Assert.notNull("poste", id);
    Assert.notNull("libelle", libelle);
    Assert.notNull("nature", nature);
  }
}
