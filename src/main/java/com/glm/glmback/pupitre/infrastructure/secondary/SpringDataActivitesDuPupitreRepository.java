package com.glm.glmback.pupitre.infrastructure.secondary;

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
    order by activite.debut, activite.id
    """
  )
  List<ActiviteDuPupitreEntity> sansFinDesSuivis(Set<UUID> suivis);

  @Query(
    """
    select activite from ActiviteDuPupitreEntity activite
    where activite.suiviId in :suivis and activite.sequenceId is not null
    order by activite.sequenceId, activite.ordreDansSequence
    """
  )
  List<ActiviteDuPupitreEntity> enConflitDesSuivis(Set<UUID> suivis);
}
