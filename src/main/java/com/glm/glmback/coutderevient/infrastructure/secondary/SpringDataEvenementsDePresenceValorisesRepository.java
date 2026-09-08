package com.glm.glmback.coutderevient.infrastructure.secondary;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataEvenementsDePresenceValorisesRepository extends JpaRepository<EvenementDePresenceValoriseEntity, UUID> {
  List<EvenementDePresenceValoriseEntity> findByJourneeIdInAndAnnulationDateIsNullOrderByDateDeSurvenueAscIdAsc(Set<UUID> journees);
}
