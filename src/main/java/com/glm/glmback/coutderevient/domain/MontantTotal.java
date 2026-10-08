package com.glm.glmback.coutderevient.domain;

import com.glm.glmback.shared.error.domain.Assert;

/** Un montant total, somme de montants deja au centime. */
public record MontantTotal(Montant valeur) {
  public MontantTotal {
    Assert.notNull("valeur", valeur);
  }

  public static MontantTotal de(Montant valeur) {
    return new MontantTotal(valeur);
  }

  public MontantTotal plus(MontantTotal autre) {
    return new MontantTotal(valeur.plus(autre.valeur));
  }
}
