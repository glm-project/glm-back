package com.glm.glmback.feuilledetemps.infrastructure.secondary;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataPointagesDAtelierDeLaFeuilleDeTempsRepository extends JpaRepository<PointageDAtelierDeLaFeuilleDeTempsEntity, UUID> {
  Optional<
    PointageDAtelierDeLaFeuilleDeTempsEntity
  > findFirstByOperateurIdAndAnnulationDateIsNullAndDateDeSurvenueBetweenOrderByDateDeSurvenueDesc(
    UUID operateurId,
    Instant debut,
    Instant fin
  );
}
