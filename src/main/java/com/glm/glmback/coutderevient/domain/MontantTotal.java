package com.glm.glmback.coutderevient.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.Optional;

/** Un total incomplet ne porte aucun chiffre, meme partiel. */
public record MontantTotal(Optional<Montant> valeur) {
  public MontantTotal {
    Assert.notNull("valeur", valeur);
  }

  public static MontantTotal de(Montant valeur) {
    return new MontantTotal(Optional.of(valeur));
  }

  public boolean complete() {
    return valeur.isPresent();
  }

  public MontantTotal plus(MontantTotal autre) {
    return new MontantTotal(valeur.flatMap(une -> autre.valeur.map(une::plus)));
  }
}
