package com.glm.glmback.feuilledetemps.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;

/**
 * Une semaine ISO, designee par son annee et son numero.
 *
 * <p>
 * L'annee est celle des semaines, pas celle du calendrier : la semaine 1 de 2026 commence le 29 decembre 2025. C'est
 * la seule definition qui donne toujours sept jours pleins, et celle que le client lit sur ses plannings.
 * </p>
 *
 * <p>
 * Aucun fuseau horaire ici : une semaine est une suite de dates, et il faut la zone de l'entreprise pour en faire des
 * instants — c'est le role de {@link DecoupageCalendaire}.
 * </p>
 */
public record SemaineCalendaire(int annee, int numero) {
  private static final int PREMIERE_SEMAINE = 1;
  private static final int DERNIERE_SEMAINE = 53;
  private static final int PREMIERE_ANNEE = 2000;
  private static final int DERNIERE_ANNEE = 2999;

  /**
   * Les bornes de l'annee ne sont pas une regle metier : elles evitent qu'un parametre aberrant recu par l'API
   * remonte en erreur technique depuis {@link LocalDate}, la ou le client attend un refus lisible.
   */
  public SemaineCalendaire {
    Assert.field("annee", annee).min(PREMIERE_ANNEE).max(DERNIERE_ANNEE);
    Assert.field("numero de semaine", numero).min(PREMIERE_SEMAINE).max(DERNIERE_SEMAINE);
  }

  /**
   * Le lundi qui ouvre la semaine, ancre sur le 4 janvier : c'est la date dont la norme garantit qu'elle tombe
   * toujours dans la semaine 1 de son annee ISO.
   */
  public LocalDate lundi() {
    return LocalDate.of(annee, 1, 4)
      .with(WeekFields.ISO.weekOfWeekBasedYear(), numero)
      .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
  }
}
