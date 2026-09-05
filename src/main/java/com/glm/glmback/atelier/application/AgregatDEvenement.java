package com.glm.glmback.atelier.application;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.UUID;

public record AgregatDEvenement(TypeDAgregatDEvenement type, UUID id) {
  public AgregatDEvenement {
    Assert.notNull("type d'agregat", type);
    Assert.notNull("id d'agregat", id);
  }
}
