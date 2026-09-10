package com.glm.glmback.syntheseheures.infrastructure.secondary;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataJourneesDeTravailSyntheseRepository extends JpaRepository<JourneeDeTravailSyntheseEntity, UUID> {
  /**
   * Recouvrir, et non commencer dans : une venue partie la veille au soir compte encore le lendemain matin. Servie
   * par {@code ix_journee_de_travail_operateur}.
   */
  @Query(
    """
    select journee
    from JourneeDeTravailSyntheseEntity journee
    where journee.operateurId = :operateurId
      and journee.debut < :finExclusive
      and (journee.fin is null or journee.fin > :debut)
    """
  )
  List<JourneeDeTravailSyntheseEntity> recouvrant(
    @Param("operateurId") UUID operateurId,
    @Param("debut") Instant debut,
    @Param("finExclusive") Instant finExclusive
  );
}
