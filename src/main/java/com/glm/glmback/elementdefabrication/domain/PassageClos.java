package com.glm.glmback.elementdefabrication.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;

public record PassageClos(
  SuiviId suivi,
  Instant misALAtelierLe,
  Instant clotureLe,
  AuteurDeLaCloture cloturePar
) implements PassageEnAtelier {
  public PassageClos {
    Assert.notNull("suivi", suivi);
    Assert.notNull("misALAtelierLe", misALAtelierLe);
    Assert.notNull("clotureLe", clotureLe);
    Assert.notNull("cloturePar", cloturePar);
  }
}
