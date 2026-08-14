package com.glm.glmback.feuilledetemps.infrastructure.secondary;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataEvenementsDePresenceLectureRepository extends JpaRepository<EvenementDePresenceLectureEntity, UUID> {
  List<EvenementDePresenceLectureEntity> findByJourneeIdInAndAnnulationDateIsNullOrderByDateDeSurvenueAsc(Set<UUID> journees);
}
