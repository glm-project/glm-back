package com.glm.glmback.syntheseheures.infrastructure.secondary;

import com.glm.glmback.syntheseheures.domain.TypeDEvenementDAtelier;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Le journal brut ne rend que des gestes : les faits de correction du gestionnaire (retrait, changement de poste) sont
 * ecartes par leur type, avant tout chargement, et ne sont donc jamais traduits en pointage.
 */
interface SpringDataPointagesDAtelierDeLaSyntheseRepository extends JpaRepository<PointageDAtelierDeLaSyntheseEntity, UUID> {
  List<
    PointageDAtelierDeLaSyntheseEntity
  > findByOperateurIdAndTypeInAndDateDeSurvenueGreaterThanEqualAndDateDeSurvenueLessThanOrderByDateDeSurvenueAscIdAsc(
    UUID operateurId,
    Collection<TypeDEvenementDAtelier> types,
    Instant debut,
    Instant finExclusive
  );
}
