package com.glm.glmback.syntheseheures.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.List;

/** Les pointages d'un element dans la periode demandee, sans interpretation. */
public record JournalDElement(ElementEngage element, List<PointageDElement> pointages) {
  public JournalDElement {
    Assert.notNull("element", element);
    Assert.notNull("pointages", pointages);
    pointages = List.copyOf(pointages);
  }
}
