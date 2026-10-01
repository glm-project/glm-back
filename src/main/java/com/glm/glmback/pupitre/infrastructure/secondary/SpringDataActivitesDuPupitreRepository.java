package com.glm.glmback.pupitre.infrastructure.secondary;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface SpringDataActivitesDuPupitreRepository extends JpaRepository<ActiviteDuPupitreEntity, UUID> {
  @Query(
    """
    select activite from ActiviteDuPupitreEntity activite
    where activite.suiviId in :suivis and activite.fin is null and activite.aResoudre = false
      and activite.debut <= :evaluation and activite.echeance > :evaluation
    order by activite.debut, activite.id
    """
  )
  List<ActiviteDuPupitreEntity> desSuivis(Set<UUID> suivis, Instant evaluation);
}
