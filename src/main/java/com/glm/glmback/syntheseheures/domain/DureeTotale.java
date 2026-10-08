package com.glm.glmback.syntheseheures.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Duration;
import java.util.List;

/** Une duree totale certaine : seules les portions terminees y contribuent. */
public record DureeTotale(Duration valeur) {
  public DureeTotale {
    Assert.notNull("valeur", valeur);
  }

  public static DureeTotale de(Duration duree) {
    return new DureeTotale(duree);
  }

  static DureeTotale somme(List<DureeTotale> durees) {
    return de(durees.stream().map(DureeTotale::valeur).reduce(Duration.ZERO, Duration::plus));
  }
}
