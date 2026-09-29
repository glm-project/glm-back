package com.glm.glmback.syntheseheures.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.List;

/** Les pointages actifs d'un element dans la periode demandee, sans interpretation. */
public record JournalDElement(ElementEngage element, List<PointageDElement> pointages) {
  public JournalDElement {
    Assert.notNull("element", element);
    Assert.notNull("pointages", pointages);
    pointages = List.copyOf(pointages);
  }

  public static JournalDElement de(ElementEngage element, List<PointageDAtelier> pointages) {
    return new JournalDElement(
      element,
      pointages
        .stream()
        .map(pointage ->
          PointageDElement.builder()
            .type(pointage.type())
            .element(element.id())
            .poste(pointage.poste())
            .nature(pointage.nature())
            .dateDeSurvenue(pointage.dateDeSurvenue())
        )
        .toList()
    );
  }
}
