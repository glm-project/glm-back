package com.glm.glmback.syntheseheures.domain;

import java.time.LocalDate;

/**
 * Une suite de jours du calendrier, du premier au dernier inclus : une semaine ISO ou un mois civil.
 *
 * <p>
 * Comme la semaine, la periode n'a pas de fuseau : c'est {@link DecoupageCalendaire} qui en fait des instants.
 * </p>
 */
public sealed interface PeriodeCalendaire permits SemaineCalendaire, MoisCalendaire {
  LocalDate premierJour();

  LocalDate dernierJour();
}
