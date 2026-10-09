package com.glm.glmback.syntheseheures.infrastructure.secondary;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataPointagesDAtelierDeLaSyntheseRepository extends JpaRepository<PointageDAtelierDeLaSyntheseEntity, UUID> {
  List<
    PointageDAtelierDeLaSyntheseEntity
  > findByOperateurIdAndDateDeSurvenueGreaterThanEqualAndDateDeSurvenueLessThanOrderByDateDeSurvenueAscIdAsc(
    UUID operateurId,
    Instant debut,
    Instant finExclusive
  );
}
