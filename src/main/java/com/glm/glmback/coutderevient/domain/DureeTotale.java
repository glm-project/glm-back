package com.glm.glmback.coutderevient.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Duration;

/** Une duree totale, somme de durees terminees. */
public record DureeTotale(Duration valeur) {
  public DureeTotale {
    Assert.notNull("valeur", valeur);
  }

  public static DureeTotale de(Duration valeur) {
    return new DureeTotale(valeur);
  }

  public DureeTotale plus(DureeTotale autre) {
    return new DureeTotale(valeur.plus(autre.valeur));
  }
}
