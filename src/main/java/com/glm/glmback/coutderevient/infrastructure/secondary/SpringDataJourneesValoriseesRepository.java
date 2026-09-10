package com.glm.glmback.coutderevient.infrastructure.secondary;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataJourneesValoriseesRepository extends JpaRepository<JourneeDeTravailValoriseeEntity, UUID> {
  /**
   * Recouvrir, et non commencer dans : une venue partie la veille au soir borne encore un travail commence le
   * lendemain matin. Servie par {@code ix_journee_de_travail_operateur}.
   */
  @Query(
    """
    select journee
    from JourneeDeTravailValoriseeEntity journee
    where journee.operateurId in :operateurs
      and journee.debut <= :fin
      and (journee.fin is null or journee.fin >= :debut)
    """
  )
  List<JourneeDeTravailValoriseeEntity> recouvrant(
    @Param("operateurs") Set<UUID> operateurs,
    @Param("debut") Instant debut,
    @Param("fin") Instant fin
  );
}
