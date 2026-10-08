package com.glm.glmback.coutderevient.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.List;
import java.util.Optional;

/** Activites terminees d'une meme nature. */
record TravailDeLaLigne(Optional<NatureDOperation> nature, List<TrancheDActivite> terminees) {
  TravailDeLaLigne {
    Assert.notNull("nature", nature);
    Assert.field("terminees", terminees).notNull().noNullElement();
    terminees = List.copyOf(terminees);
  }
}
