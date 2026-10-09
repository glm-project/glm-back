package com.glm.glmback.naturedetravail.infrastructure.secondary;

import com.glm.glmback.naturedetravail.domain.NatureDeTravailId;
import com.glm.glmback.naturedetravail.domain.NaturesEnUsage;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

/**
 * Ce qui empeche de supprimer une nature : les postes de l'entreprise courante qui la portent.
 */
@Repository
class PostesDesNatures implements NaturesEnUsage {

  private final SpringDataPostesDesNaturesRepository postes;

  PostesDesNatures(SpringDataPostesDesNaturesRepository postes) {
    this.postes = postes;
  }

  @Override
  public boolean estUtilisee(NatureDeTravailId nature) {
    return postes.findFirstByNatureId(nature.uuid()).isPresent();
  }

  @Override
  public Set<NatureDeTravailId> utiliseesParmi(Collection<NatureDeTravailId> natures) {
    if (natures.isEmpty()) {
      return Set.of();
    }

    return postes
      .findNaturesPorteesParmi(natures.stream().map(NatureDeTravailId::uuid).toList())
      .stream()
      .map(NatureDeTravailId::new)
      .collect(Collectors.toUnmodifiableSet());
  }
}
