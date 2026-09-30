package com.glm.glmback.syntheseheures.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/** Un total certain porte sa duree ; un total incomplet ne porte aucun chiffre. */
public record DureeTotale(Optional<Duration> valeur) {
  public DureeTotale {
    Assert.notNull("valeur", valeur);
  }

  public static DureeTotale de(Duration duree) {
    return new DureeTotale(Optional.of(duree));
  }

  public static DureeTotale incomplete() {
    return new DureeTotale(Optional.empty());
  }

  public boolean complete() {
    return valeur.isPresent();
  }

  static DureeTotale somme(List<DureeTotale> durees) {
    return durees.stream().allMatch(DureeTotale::complete)
      ? de(
          durees
            .stream()
            .map(duree -> duree.valeur().orElseThrow())
            .reduce(Duration.ZERO, Duration::plus)
        )
      : incomplete();
  }
}
