package com.glm.glmback.coutderevient.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;

/**
 * Une tranche d'activite et le diviseur qui s'y applique : tout ce qu'il faut pour la chiffrer.
 *
 * <p>
 * Le diviseur ne concerne que la main d'oeuvre. Le cout de la machine court en entier sur la meme tranche, et c'est
 * pourquoi les deux restent separes jusqu'au bout.
 * </p>
 *
 * <p>
 * Les deux montants sortent d'ici a l'echelle de travail, pas encore arrondis : le rapport n'arrondit qu'une fois la
 * ligne entiere sommee, pour que son total soit exactement la somme de ce qu'il affiche.
 * </p>
 */
public record TrancheValorisable(TrancheDActivite tranche, Diviseur diviseur) {
  private static final BigDecimal MILLISECONDES_PAR_HEURE = new BigDecimal(3_600_000);
  private static final int ECHELLE_DE_TRAVAIL = 6;

  public TrancheValorisable {
    Assert.notNull("tranche", tranche);
    Assert.notNull("diviseur", diviseur);
  }

  public Activite activite() {
    return tranche.activite();
  }

  public Duration duree() {
    return tranche.duree();
  }

  /**
   * Ce que la machine a coute pendant cette tranche, jamais divise : le client enonce la regle deux fois, chaque
   * machine active court en entier. Rien quand le poste n'est pas valorise, ou qu'il n'y a pas de poste.
   */
  public BigDecimal coutMachine() {
    return activite()
      .coutHoraire()
      .map(cout -> cout.value().multiply(heures()))
      .orElse(BigDecimal.ZERO);
  }

  /**
   * Ce que la personne a coute pendant cette tranche, divise par le nombre de postes qu'elle occupait alors : elle ne
   * peut pas etre payee deux fois la meme heure. Rien quand l'operateur n'est pas valorise.
   */
  public BigDecimal coutDeMainDOeuvre() {
    return activite()
      .tauxHoraire()
      .map(taux -> taux.value().multiply(heures()).divide(new BigDecimal(diviseur.value()), ECHELLE_DE_TRAVAIL, RoundingMode.HALF_UP))
      .orElse(BigDecimal.ZERO);
  }

  private BigDecimal heures() {
    return new BigDecimal(duree().toMillis()).divide(MILLISECONDES_PAR_HEURE, ECHELLE_DE_TRAVAIL, RoundingMode.HALF_UP);
  }
}
