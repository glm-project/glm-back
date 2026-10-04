package com.glm.glmback.atelier.infrastructure.secondary;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

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

  @Query("select distinct suivi from SuiviDAtelierEntity suivi left join fetch suivi.journal where suivi.id = :id")
  Optional<SuiviDAtelierEntity> findWithJournalById(UUID id);

  Optional<SuiviDAtelierEntity> findFirstByElementIdAndClotureDateDeSurvenueIsNullOrderByEngagementDateDescIdAsc(UUID elementId);
}
