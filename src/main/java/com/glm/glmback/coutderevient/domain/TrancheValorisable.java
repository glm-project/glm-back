package com.glm.glmback.coutderevient.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;

/**
 * Une tranche d'activite et le diviseur qui s'y applique : tout ce qu'il faut pour la chiffrer.
 *
 * <p>
 * Le diviseur ne concerne que la main d'oeuvre. La machine, jamais partagee, se chiffre sur la tranche entiere
 * ({@link TrancheDActivite#coutMachine()}) : elle n'a rien a faire d'un decoupage qui ne la concerne pas.
 * </p>
 *
 * <p>
 * La main d'oeuvre sort d'ici a l'echelle de travail, pas encore arrondie : la ligne l'arrondit une fois sommee.
 * </p>
 */
public record TrancheValorisable(TrancheDActivite tranche, Optional<Diviseur> diviseur, Set<ActiviteInterpretee> responsables) {
  private static final BigDecimal MILLISECONDES_PAR_HEURE = new BigDecimal(3_600_000);
  private static final int ECHELLE_DE_TRAVAIL = 6;

  public TrancheValorisable {
    Assert.notNull("tranche", tranche);
    Assert.notNull("diviseur", diviseur);
    Assert.field("responsables", responsables).notNull().noNullElement();
  }

  public Activite activite() {
    return tranche.activite();
  }

  public Duration duree() {
    return tranche.duree();
  }

  /**
   * Ce que la personne a coute pendant cette tranche, divise par le nombre de postes qu'elle occupait alors : elle ne
   * peut pas etre payee deux fois la meme heure. Rien quand l'operateur n'est pas valorise.
   */
  public Optional<BigDecimal> coutDeMainDOeuvre() {
    Optional<TauxHoraire> taux = activite().tauxHoraire();
    if (taux.isEmpty()) {
      return Optional.of(BigDecimal.ZERO);
    }
    return diviseur.map(partage ->
      taux.orElseThrow().value().multiply(heures()).divide(new BigDecimal(partage.value()), ECHELLE_DE_TRAVAIL, RoundingMode.HALF_UP)
    );
  }

  private BigDecimal heures() {
    return new BigDecimal(duree().toMillis()).divide(MILLISECONDES_PAR_HEURE, ECHELLE_DE_TRAVAIL, RoundingMode.HALF_UP);
  }
}
