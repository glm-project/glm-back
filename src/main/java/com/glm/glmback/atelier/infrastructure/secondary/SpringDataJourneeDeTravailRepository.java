package com.glm.glmback.atelier.infrastructure.secondary;

import com.glm.glmback.atelier.domain.EtatDePresence;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;

interface SpringDataJourneeDeTravailRepository
  extends JpaRepository<JourneeDeTravailEntity, UUID>, JpaSpecificationExecutor<JourneeDeTravailEntity>
{
  /**
   * Charge une journee pour ecriture, en serialisant les redacteurs sur sa ligne — meme raison que pour un suivi
   * d'atelier, un poste d'atelier partage laissant plusieurs saisies viser la meme journee.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<JourneeDeTravailEntity> findForUpdateById(UUID id);

  Optional<JourneeDeTravailEntity> findFirstByOperateurAndEtatNotOrderByDebutDescIdAsc(String operateur, EtatDePresence etat);
}
