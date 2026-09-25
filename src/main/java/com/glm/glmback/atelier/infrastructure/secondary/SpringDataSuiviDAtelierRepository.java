package com.glm.glmback.atelier.infrastructure.secondary;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataSuiviDAtelierRepository
  extends JpaRepository<SuiviDAtelierEntity, UUID>, JpaSpecificationExecutor<SuiviDAtelierEntity>
{
  /**
   * Charge un suivi pour ecriture, en serialisant les redacteurs sur sa ligne.
   *
   * <p>
   * Sans ce verrou, deux pointages simultanes liraient chacun le journal d'avant l'autre, et le controle
   * d'obsolescence ne verrait rien : leurs deux lectures precederaient les deux ecritures. Le verrou les met en file,
   * ce qui donne au second un journal a jour — donc un ecart a constater.
   * </p>
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<SuiviDAtelierEntity> findForUpdateById(UUID id);

  Optional<SuiviDAtelierEntity> findFirstByElementIdAndClotureDateDeSurvenueIsNullOrderByEngagementDateDescIdAsc(UUID elementId);

  /**
   * Le dernier pointage actif d'un operateur sur la periode, tous elements confondus : la matiere de la fin presumee
   * d'une journee abandonnee.
   */
  @Query(
    """
    select max(evenement.dateDeSurvenue) from EvenementDAtelierEntity evenement
    where evenement.operateurId = :operateur
      and evenement.annulationDate is null
      and evenement.dateDeSurvenue between :debut and :fin
    """
  )
  Optional<Instant> dernierPointageDe(@Param("operateur") UUID operateur, @Param("debut") Instant debut, @Param("fin") Instant fin);
}
