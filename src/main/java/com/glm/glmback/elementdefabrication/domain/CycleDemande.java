package com.glm.glmback.elementdefabrication.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.Optional;

/**
 * Le cycle lu, et pour les clotures seulement, la periode de cloture : la liste des clotures grandit sans fin.
 */
public record CycleDemande(CycleDeVie cycle, Optional<Periode> cloture) {
  public CycleDemande {
    Assert.notNull("cycle", cycle);
    Assert.notNull("cloture", cloture);
    if (cloture.isPresent() && cycle != CycleDeVie.CLOTURE) {
      throw new ClotureHorsDesCloturesException();
    }
  }

  /**
   * Une periode de cloture sans cycle demande est refusee comme avec un autre cycle.
   */
  public static Optional<CycleDemande> de(Optional<CycleDeVie> cycle, Optional<Periode> cloture) {
    if (cycle.isEmpty() && cloture.isPresent()) {
      throw new ClotureHorsDesCloturesException();
    }
    return cycle.map(demande -> new CycleDemande(demande, cloture));
  }

  public boolean matches(ElementDansSonCycle lu) {
    return cycle == lu.cycle() && cloture.map(periode -> clotureDans(periode, lu)).orElse(true);
  }

  private static boolean clotureDans(Periode periode, ElementDansSonCycle lu) {
    return lu
      .passage()
      .filter(PassageClos.class::isInstance)
      .map(clos -> periode.contains(((PassageClos) clos).clotureLe()))
      .orElse(false);
  }
}
