package com.glm.glmback.naturedetravail.infrastructure.secondary;

import com.glm.glmback.naturedetravail.domain.NatureDeTravailId;
import com.glm.glmback.naturedetravail.domain.NaturesEnUsage;
import java.util.Collection;
import java.util.Set;
import org.springframework.stereotype.Repository;

/**
 * Rien ne reference encore une nature : les postes portent la leur en texte libre. Cet adapter cedera la place a la
 * lecture des postes, puis des pointages, quand ils porteront l'identifiant de leur nature.
 */
@Repository
class NaturesSansUsage implements NaturesEnUsage {

  @Override
  public Set<NatureDeTravailId> utiliseesParmi(Collection<NatureDeTravailId> natures) {
    return Set.of();
  }
}
