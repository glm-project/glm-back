package com.glm.glmback.syntheseheures.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Un mois civil, designe par son annee et son numero de 1 a 12.
 *
 * <p>
 * Une semaine a cheval sur deux mois y est coupee : le mois va de son premier a son dernier jour, quel que soit le
 * jour de la semaine.
 * </p>
 */
public record MoisCalendaire(int annee, int mois) implements PeriodeCalendaire {
  private static final int PREMIER_MOIS = 1;
  private static final int DERNIER_MOIS = 12;
  private static final int PREMIERE_ANNEE = 2000;
  private static final int DERNIERE_ANNEE = 2999;

  /** Memes bornes d'annee que {@link SemaineCalendaire}, pour la meme raison. */
  public MoisCalendaire {
    Assert.field("annee", annee).min(PREMIERE_ANNEE).max(DERNIERE_ANNEE);
    Assert.field("mois", mois).min(PREMIER_MOIS).max(DERNIER_MOIS);
  }

  @Override
  public LocalDate premierJour() {
    return LocalDate.of(annee, mois, 1);
  }

  @Override
  public LocalDate dernierJour() {
    return YearMonth.of(annee, mois).atEndOfMonth();
  }
}
