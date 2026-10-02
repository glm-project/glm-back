package com.glm.glmback.coutderevient.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.Optional;

/**
 * Une activite et la periode fermee pendant laquelle elle a couru : la matiere du calcul.
 *
 * <p>
 * C'est ce que devient un activite terminee une fois le temps arrete. Tout ce qui suit — decoupage sur les
 * sous-periodes de parallelisme, valorisation, agregation par nature — ne travaille plus que sur des tranches, donc
 * sur des durees connues.
 * </p>
 */
public record TrancheDActivite(Activite activite, Periode periode, boolean finAutomatique) {
  private static final BigDecimal MILLISECONDES_PAR_HEURE = new BigDecimal(3_600_000);
  private static final int ECHELLE_DE_TRAVAIL = 10;

  public TrancheDActivite {
    Assert.notNull("activite", activite);
    Assert.notNull("periode", periode);
  }

  public TrancheDActivite(Activite activite, Periode periode) {
    this(activite, periode, false);
  }

  public Duration duree() {
    return periode.duree();
  }

  /**
   * Ce que la machine a coute pendant toute la tranche, arrondi une seule fois au centime. La machine n'est jamais
   * partagee : la decouper pour l'arrondir ferait deriver son cout pour une raison qui lui est etrangere. Rien quand
   * le poste n'est pas valorise, ou qu'il n'y a pas de poste.
   */
  public Montant coutMachine() {
    return activite
      .coutHoraire()
      .map(cout ->
        new Montant(
          cout
            .value()
            .multiply(new BigDecimal(duree().toMillis()))
            .divide(MILLISECONDES_PAR_HEURE, ECHELLE_DE_TRAVAIL, RoundingMode.HALF_UP)
        )
      )
      .orElse(Montant.ZERO);
  }

  public OperateurId operateur() {
    return activite.operateur();
  }

  /**
   * La meme tranche reduite a la periode donnee, s'il en reste quelque chose. C'est par la que passe le decoupage sur
   * les sous-periodes ou le diviseur est constant.
   */
  public Optional<TrancheDActivite> reduiteA(Periode autre) {
    return periode.intersection(autre).map(part -> new TrancheDActivite(activite, part, finAutomatique));
  }
}
