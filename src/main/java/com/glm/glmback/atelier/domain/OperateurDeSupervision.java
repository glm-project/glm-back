package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.List;

public record OperateurDeSupervision(OperateurConnu operateur, List<NatureDOperation> metiers) {
  public OperateurDeSupervision {
    Assert.notNull("operateur", operateur);
    Assert.field("metiers", metiers).notNull().noNullElement();
    metiers = List.copyOf(metiers);
  }
}
