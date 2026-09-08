package com.glm.glmback.coutderevient.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Un intervalle ferme des deux bouts, donc mesurable.
 *
 * <p>
 * C'est ce qui la separe de {@link Plage} : tant qu'un travail n'est pas arrete, il n'a pas de duree, et rien ne se
 * valorise. La fermeture a l'horloge est le passage de l'une a l'autre.
 * </p>
 */
public record Periode(Instant debut, Instant fin) {
  public Periode {
    Assert.notNull("debut", debut);
    Assert.field("fin", fin).afterOrAt(debut);
  }

  public Duration duree() {
    return Duration.between(debut, fin);
  }

  /**
   * La part commune aux deux periodes, s'il y en a une de duree non nulle.
   *
   * <p>
   * Deux periodes qui ne font que se toucher n'en partagent aucune : une pause prise a la seconde ou le travail
   * commence ne doit pas ouvrir une ligne de rapport sur du vide.
   * </p>
   */
  public Optional<Periode> intersection(Periode autre) {
    Instant debutCommun = debut.isAfter(autre.debut) ? debut : autre.debut;
    Instant finCommune = fin.isBefore(autre.fin) ? fin : autre.fin;

    if (!finCommune.isAfter(debutCommun)) {
      return Optional.empty();
    }

    return Optional.of(new Periode(debutCommun, finCommune));
  }
}
