package com.glm.glmback.coutderevient.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.Optional;
import java.util.Set;

/**
 * Une periode pendant laquelle le nombre de postes occupes par un operateur ne change pas.
 *
 * <p>
 * C'est l'unite du calcul de main d'oeuvre : le diviseur ne vaut que la ou il est constant, et decouper aux bornes de
 * tous les pointages est ce qui garantit qu'il l'est. Un diviseur pris sur l'intervalle entier serait faux des que
 * deux pointages ne commencent pas ensemble.
 * </p>
 */
public record SousPeriode(Periode periode, Optional<Diviseur> diviseur, Set<ActiviteInterpretee> responsables) {
  public SousPeriode {
    Assert.notNull("periode", periode);
    Assert.notNull("diviseur", diviseur);
    Assert.field("responsables", responsables).notNull().noNullElement();
  }

  public SousPeriode(Periode periode, Diviseur diviseur) {
    this(periode, Optional.of(diviseur), Set.of());
  }
}
