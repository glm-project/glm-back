package com.glm.glmback.syntheseheures.infrastructure.secondary;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataPointagesDAtelierDeLaSyntheseRepository extends JpaRepository<PointageDAtelierDeLaSyntheseEntity, UUID> {
  /**
   * Le dernier pointage actif d'un operateur sur la periode, bornes comprises, tous elements confondus.
   */
  Optional<
    PointageDAtelierDeLaSyntheseEntity
  > findFirstByOperateurIdAndAnnulationDateIsNullAndDateDeSurvenueBetweenOrderByDateDeSurvenueDesc(
    UUID operateurId,
    Instant debut,
    Instant fin
  );
}
