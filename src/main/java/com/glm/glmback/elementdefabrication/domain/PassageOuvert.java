package com.glm.glmback.elementdefabrication.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;

public record PassageOuvert(SuiviId suivi, Instant misALAtelierLe, EtatEnAtelier etat) implements PassageEnAtelier {
  public PassageOuvert {
    Assert.notNull("suivi", suivi);
    Assert.notNull("misALAtelierLe", misALAtelierLe);
    Assert.notNull("etat", etat);
  }
}
