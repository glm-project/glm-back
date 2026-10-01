package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.Optional;

public record ActiviteDeSupervision(
  DescriptionDActiviteDeSupervision description,
  EtatDActiviteDeSupervision etat,
  Optional<Instant> finRetenue
) {
  public ActiviteDeSupervision {
    Assert.notNull("description", description);
    Assert.notNull("etat", etat);
    Assert.notNull("fin retenue", finRetenue);
  }

  public static ActiviteDeSupervision a(DescriptionDActiviteDeSupervision description, Instant evaluation) {
    if (!evaluation.isBefore(description.echeance())) {
      return new ActiviteDeSupervision(
        description,
        EtatDActiviteDeSupervision.TERMINEE_AUTOMATIQUEMENT,
        Optional.of(description.echeance())
      );
    }
    return new ActiviteDeSupervision(description, EtatDActiviteDeSupervision.EN_COURS, Optional.empty());
  }
}
