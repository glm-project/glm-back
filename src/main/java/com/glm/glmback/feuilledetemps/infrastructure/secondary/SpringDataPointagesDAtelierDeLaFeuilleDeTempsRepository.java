package com.glm.glmback.feuilledetemps.infrastructure.secondary;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataPointagesDAtelierDeLaFeuilleDeTempsRepository extends JpaRepository<PointageDAtelierDeLaFeuilleDeTempsEntity, UUID> {
  /**
   * Le dernier pointage actif d'un operateur sur la periode, bornes comprises, tous elements confondus.
   */
  Optional<
    PointageDAtelierDeLaFeuilleDeTempsEntity
  > findFirstByOperateurIdAndAnnulationDateIsNullAndDateDeSurvenueBetweenOrderByDateDeSurvenueDesc(
    UUID operateurId,
    Instant debut,
    Instant fin
  );
}
