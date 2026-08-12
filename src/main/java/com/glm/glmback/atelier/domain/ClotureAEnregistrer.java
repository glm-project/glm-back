package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.Optional;

public record ClotureAEnregistrer(SuiviDAtelierId suivi, Auteur auteur, Optional<Instant> dateDeSurvenue) {
  public ClotureAEnregistrer {
    Assert.notNull("suivi", suivi);
    Assert.notNull("auteur", auteur);
    Assert.notNull("dateDeSurvenue", dateDeSurvenue);
  }

  public ClotureAEnregistrer(SuiviDAtelierId suivi, Auteur auteur) {
    this(suivi, auteur, Optional.empty());
  }
}
