package com.glm.glmback.coutderevient.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.Optional;

/**
 * Un intervalle dont la fin n'est pas forcement connue : une fenetre de presence, une tranche de travail.
 *
 * <p>
 * Une plage sans fin n'est pas une anomalie — l'operateur n'a simplement pas encore arrete. C'est
 * {@link #fermee(Instant)} qui tranche, a l'instant de la lecture, et qui rend une {@link Periode} mesurable.
 * </p>
 */
public record Plage(Instant debut, Optional<Instant> fin) {
  public Plage {
    Assert.notNull("debut", debut);
    Assert.notNull("fin", fin);
    fin.ifPresent(date -> Assert.field("fin", date).afterOrAt(debut));
  }

  /**
   * La part commune aux deux plages, s'il en reste une de duree non nulle. Deux plages encore ouvertes se croisent en
   * une plage encore ouverte.
   */
  public Optional<Plage> intersection(Plage autre) {
    Instant debutCommun = debut.isAfter(autre.debut) ? debut : autre.debut;
    Optional<Instant> finCommune = plusTot(fin, autre.fin);

    if (finCommune.filter(date -> !date.isAfter(debutCommun)).isPresent()) {
      return Optional.empty();
    }

    return Optional.of(new Plage(debutCommun, finCommune));
  }

  /**
   * La periode que devient cette plage si on arrete le temps a l'instant donne. Une plage deja fermee garde sa fin.
   */
  public Periode fermee(Instant maintenant) {
    return new Periode(debut, fin.orElse(maintenant));
  }

  private static Optional<Instant> plusTot(Optional<Instant> une, Optional<Instant> autre) {
    return une.map(date -> autre.filter(date::isAfter).orElse(date)).or(() -> autre);
  }
}
