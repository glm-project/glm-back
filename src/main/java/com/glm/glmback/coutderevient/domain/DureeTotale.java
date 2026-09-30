package com.glm.glmback.coutderevient.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Duration;
import java.util.Optional;

/** Un total incomplet ne porte aucun chiffre, meme partiel. */
public record DureeTotale(Optional<Duration> valeur) {
  public DureeTotale {
    Assert.notNull("valeur", valeur);
  }

  public static DureeTotale de(Duration valeur) {
    return new DureeTotale(Optional.of(valeur));
  }

  public static DureeTotale incomplet() {
    return new DureeTotale(Optional.empty());
  }

  public boolean complete() {
    return valeur.isPresent();
  }

  public DureeTotale plus(DureeTotale autre) {
    return new DureeTotale(valeur.flatMap(une -> autre.valeur.map(une::plus)));
  }
}
