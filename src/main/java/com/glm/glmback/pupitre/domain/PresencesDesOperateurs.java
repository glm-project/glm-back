package com.glm.glmback.pupitre.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.Map;

/**
 * La presence de chaque operateur a l'instant du referentiel. Un operateur qui n'y figure pas est absent.
 */
public record PresencesDesOperateurs(Map<OperateurId, PresenceDuPupitre> presences) {
  public PresencesDesOperateurs {
    Assert.notNull("presences", presences);
    presences = Map.copyOf(presences);
  }

  public PresenceDuPupitre de(OperateurId operateur) {
    return presences.getOrDefault(operateur, PresenceDuPupitre.absente());
  }
}
