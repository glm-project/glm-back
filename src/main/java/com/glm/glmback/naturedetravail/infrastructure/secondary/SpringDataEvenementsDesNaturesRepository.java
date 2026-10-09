package com.glm.glmback.naturedetravail.infrastructure.secondary;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface SpringDataEvenementsDesNaturesRepository extends JpaRepository<EvenementDeLaNatureEntity, UUID> {
  Optional<EvenementDeLaNatureEntity> findFirstByNatureId(UUID natureId);

  @Query("select distinct evenement.natureId from EvenementDeLaNatureEntity evenement where evenement.natureId in :natures")
  Set<UUID> findNaturesPointeesParmi(Collection<UUID> natures);
}
