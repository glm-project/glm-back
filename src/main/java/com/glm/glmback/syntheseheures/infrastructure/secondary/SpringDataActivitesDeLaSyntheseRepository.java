package com.glm.glmback.syntheseheures.infrastructure.secondary;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface SpringDataActivitesDeLaSyntheseRepository extends JpaRepository<ActiviteDeLaSyntheseEntity, UUID> {
  @Query(
    """
    select activite from ActiviteDeLaSyntheseEntity activite
    join fetch activite.suivi
    where activite.operateurId = :operateur
      and activite.debut < :finExclusive
      and activite.aResoudre = false
      and coalesce(activite.fin, activite.echeance) > :debut
    order by activite.debut, activite.id
    """
  )
  List<ActiviteDeLaSyntheseEntity> recouvrant(UUID operateur, Instant debut, Instant finExclusive);
}
