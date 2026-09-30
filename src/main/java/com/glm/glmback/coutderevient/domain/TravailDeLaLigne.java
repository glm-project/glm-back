package com.glm.glmback.coutderevient.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.List;
import java.util.Optional;

/** Activites d'une meme nature, certaines et a resoudre separees. */
record TravailDeLaLigne(Optional<NatureDOperation> nature, List<TrancheDActivite> terminees, List<ActiviteInterpretee> aResoudre) {
  TravailDeLaLigne {
    Assert.notNull("nature", nature);
    Assert.field("terminees", terminees).notNull().noNullElement();
    Assert.field("a resoudre", aResoudre).notNull().noNullElement();
    terminees = List.copyOf(terminees);
    aResoudre = List.copyOf(aResoudre);
  }
}
