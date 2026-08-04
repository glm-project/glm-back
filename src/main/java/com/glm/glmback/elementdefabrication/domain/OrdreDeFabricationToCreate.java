package com.glm.glmback.elementdefabrication.domain;

import com.glm.glmback.shared.error.domain.Assert;

public record OrdreDeFabricationToCreate(Titre titre, Description description) implements ElementDeFabricationToCreate {
  public OrdreDeFabricationToCreate {
    Assert.notNull("titre", titre);
    Assert.notNull("description", description);
  }
}
