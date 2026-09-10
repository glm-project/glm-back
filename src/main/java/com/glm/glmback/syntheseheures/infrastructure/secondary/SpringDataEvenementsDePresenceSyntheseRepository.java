package com.glm.glmback.syntheseheures.infrastructure.secondary;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataEvenementsDePresenceSyntheseRepository extends JpaRepository<EvenementDePresenceSyntheseEntity, UUID> {
  List<EvenementDePresenceSyntheseEntity> findByJourneeIdInAndAnnulationDateIsNullOrderByDateDeSurvenueAsc(Set<UUID> journees);
}
