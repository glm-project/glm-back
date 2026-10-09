package com.glm.glmback.naturedetravail.infrastructure.secondary;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface SpringDataPostesDesNaturesRepository extends JpaRepository<PosteDeLaNatureEntity, UUID> {
  Optional<PosteDeLaNatureEntity> findFirstByNatureId(UUID natureId);

  @Query("select distinct poste.natureId from PosteDeLaNatureEntity poste where poste.natureId in :natures")
  Set<UUID> findNaturesPorteesParmi(Collection<UUID> natures);

  @Query(
    "select poste.natureId as natureId, count(poste) as postes from PosteDeLaNatureEntity poste where poste.natureId in :natures group by poste.natureId"
  )
  List<PostesDUneNature> countPostesParmi(Collection<UUID> natures);

  interface PostesDUneNature {
    UUID getNatureId();

    long getPostes();
  }
}
