package com.glm.glmback.pupitre.infrastructure.secondary;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface SpringDataSuivisDuPupitreRepository extends JpaRepository<SuiviDuPupitreEntity, UUID> {
  @Query(
    """
    select suivi
    from SuiviDuPupitreEntity suivi
    where suivi.clotureDateDeSurvenue is null
    order by suivi.elementNom, suivi.id
    """
  )
  List<SuiviDuPupitreEntity> ouverts();

  @Query(
    value = "select distinct suivi_id from evenement_d_atelier where suivi_id in :suivis and annulation_date is null",
    nativeQuery = true
  )
  Set<UUID> suivisPointes(Set<UUID> suivis);
}
