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
    """
    select distinct evenement.suiviId from EvenementDuPupitreEntity evenement
    where evenement.suiviId in :suivis and evenement.annulationDate is null
    """
  )
  Set<UUID> avecPointages(Set<UUID> suivis);
}
