package com.glm.glmback.pupitre.infrastructure.secondary;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface SpringDataEvenementsDuPupitreRepository extends JpaRepository<EvenementDuPupitreEntity, UUID> {
  /**
   * L'ordre est celui du repli : date de survenue, puis date d'enregistrement, puis identifiant. Le dernier terme
   * n'est pas cosmetique — a horodatage identique, l'ordre departagerait au hasard sans lui, et l'automate d'etat en
   * dependrait.
   */
  @Query(
    """
    select evenement
    from EvenementDuPupitreEntity evenement
    where evenement.suiviId in :suivis and evenement.annulationDate is null
    order by evenement.dateDeSurvenue, evenement.dateDEnregistrement, evenement.id
    """
  )
  List<EvenementDuPupitreEntity> desSuivis(Set<UUID> suivis);
}
