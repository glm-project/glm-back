package com.glm.glmback.coutderevient.infrastructure.secondary;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface SpringDataActivitesValoriseesRepository extends JpaRepository<ActiviteValoriseeEntity, UUID> {
  @Query(
    """
    select activite from ActiviteValoriseeEntity activite join fetch activite.ouverture join fetch activite.suivi
    where activite.suivi.elementId = :element and activite.aResoudre = false order by activite.debut, activite.id
    """
  )
  List<ActiviteValoriseeEntity> de(UUID element);

  @Query(
    """
    select activite from ActiviteValoriseeEntity activite join fetch activite.ouverture join fetch activite.suivi
    where activite.operateurId in :operateurs and activite.aResoudre = false and activite.debut <= :fin
      and coalesce(activite.fin, activite.echeance) >= :debut
    order by activite.debut, activite.id
    """
  )
  List<ActiviteValoriseeEntity> occupation(Set<UUID> operateurs, Instant debut, Instant fin);
}
