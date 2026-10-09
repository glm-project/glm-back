package com.glm.glmback.naturedetravail.infrastructure.secondary;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface SpringDataPostesDesNaturesRepository extends JpaRepository<PosteDeLaNatureEntity, UUID> {
  Optional<PosteDeLaNatureEntity> findFirstByNatureId(UUID natureId);

  @Query("select distinct poste.natureId from PosteDeLaNatureEntity poste where poste.natureId in :natures")
  Set<UUID> findNaturesPorteesParmi(Collection<UUID> natures);
}
