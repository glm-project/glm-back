package com.glm.glmback.naturedetravail.infrastructure.secondary;

import com.glm.glmback.naturedetravail.domain.NatureDeTravailId;
import com.glm.glmback.naturedetravail.domain.NaturesEnUsage;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Repository;

/**
 * Ce qui empeche de supprimer une nature, dans l'entreprise courante : les postes qui la portent et les pointages qui
 * l'ont recopiee.
 */
@Repository
class UsagesDesNatures implements NaturesEnUsage {

  private final SpringDataPostesDesNaturesRepository postes;
  private final SpringDataEvenementsDesNaturesRepository evenements;

  UsagesDesNatures(SpringDataPostesDesNaturesRepository postes, SpringDataEvenementsDesNaturesRepository evenements) {
    this.postes = postes;
    this.evenements = evenements;
  }

  @Override
  public boolean estUtilisee(NatureDeTravailId nature) {
    return postes.findFirstByNatureId(nature.uuid()).isPresent();
  }

  @Override
  public boolean estPointee(NatureDeTravailId nature) {
    return evenements.findFirstByNatureId(nature.uuid()).isPresent();
  }

  @Override
  public Set<NatureDeTravailId> utiliseesParmi(Collection<NatureDeTravailId> natures) {
    if (natures.isEmpty()) {
      return Set.of();
    }
    Collection<UUID> uuids = natures.stream().map(NatureDeTravailId::uuid).toList();

    return Stream.concat(postes.findNaturesPorteesParmi(uuids).stream(), evenements.findNaturesPointeesParmi(uuids).stream())
      .map(NatureDeTravailId::new)
      .collect(Collectors.toUnmodifiableSet());
  }
}
