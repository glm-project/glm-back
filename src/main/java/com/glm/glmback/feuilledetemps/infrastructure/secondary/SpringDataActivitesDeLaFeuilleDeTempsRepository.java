package com.glm.glmback.feuilledetemps.infrastructure.secondary;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface SpringDataActivitesDeLaFeuilleDeTempsRepository extends JpaRepository<ActiviteDeLaFeuilleDeTempsEntity, UUID> {
  @Query(
    """
    select activite from ActiviteDeLaFeuilleDeTempsEntity activite
    join fetch activite.suivi
    where activite.operateurId = :operateur
      and activite.debut < :finExclusive
      and coalesce(activite.fin, activite.echeance) > :debut
    order by activite.debut, activite.id
    """
  )
  List<ActiviteDeLaFeuilleDeTempsEntity> recouvrant(UUID operateur, Instant debut, Instant finExclusive);
}
