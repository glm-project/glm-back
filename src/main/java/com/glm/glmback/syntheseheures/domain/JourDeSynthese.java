package com.glm.glmback.syntheseheures.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.LocalDate;
import java.util.List;

/** Un jour du calendrier, son journal brut et son temps operationnel. */
public record JourDeSynthese(LocalDate jour, List<PointageDElement> pointages, DureeTotale dureeOperationnelle) {
  public JourDeSynthese {
    Assert.notNull("jour", jour);
    Assert.field("pointages", pointages).notNull().noNullElement();
    Assert.notNull("duree operationnelle", dureeOperationnelle);
  }

  static JourBuilder builder() {
    return jour -> pointages -> dureeOperationnelle -> new JourDeSynthese(jour, pointages, dureeOperationnelle);
  }

  interface JourBuilder {
    PointagesBuilder jour(LocalDate jour);
  }

  interface PointagesBuilder {
    DureeBuilder pointages(List<PointageDElement> pointages);
  }

  interface DureeBuilder {
    JourDeSynthese dureeOperationnelle(DureeTotale dureeOperationnelle);
  }
}
