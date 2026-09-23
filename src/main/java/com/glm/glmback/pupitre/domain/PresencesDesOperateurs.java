package com.glm.glmback.pupitre.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.Map;

public record PresencesDesOperateurs(Map<OperateurId, EtatDePresence> presences) {
  public PresencesDesOperateurs {
    Assert.notNull("presences", presences);
    presences = Map.copyOf(presences);
  }

  public EtatDePresence de(OperateurId operateur) {
    return presences.getOrDefault(operateur, EtatDePresence.ABSENT);
  }
}
