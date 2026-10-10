package com.glm.glmback.postedetravail.domain;

import java.util.Optional;

/**
 * Ce que ce contexte sait du referentiel des natures de travail, sans jamais dependre de son contexte.
 */
public interface NaturesDeclarees {
  Optional<NatureDuPoste> get(NatureDeTravailId id);
}
