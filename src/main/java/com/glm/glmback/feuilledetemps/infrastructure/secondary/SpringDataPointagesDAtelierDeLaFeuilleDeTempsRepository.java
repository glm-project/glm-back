package com.glm.glmback.feuilledetemps.infrastructure.secondary;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataPointagesDAtelierDeLaFeuilleDeTempsRepository extends JpaRepository<PointageDAtelierDeLaFeuilleDeTempsEntity, UUID> {
  /**
   * Le dernier pointage actif d'un operateur sur la periode, bornes comprises, tous elements confondus.
   */
  Optional<
    PointageDAtelierDeLaFeuilleDeTempsEntity
  > findFirstByOperateurIdAndAnnulationDateIsNullAndDateDeSurvenueBetweenOrderByDateDeSurvenueDesc(
    UUID operateurId,
    Instant debut,
    Instant fin
  );

  /**
   * Les passages en atelier ou l'operateur a pointe sur la periode, servis par {@code ix_evenement_d_atelier_operateur}.
   */
  @Query(
    """
    select distinct pointage.suiviId
    from PointageDAtelierDeLaFeuilleDeTempsEntity pointage
    where pointage.operateurId = :operateurId
      and pointage.dateDeSurvenue >= :depuis
      and pointage.dateDeSurvenue < :finExclusive
      and pointage.annulationDate is null
    """
  )
  Set<UUID> suivisDeLOperateur(
    @Param("operateurId") UUID operateurId,
    @Param("depuis") Instant depuis,
    @Param("finExclusive") Instant finExclusive
  );

  /**
   * Le tri porte aussi sur l'identifiant : a horodatage egal, l'ordre du journal se departagerait au hasard, et le
   * repli du domaine, qui trie de facon stable sur la seule date de survenue, conserve celui-ci.
   */
  List<PointageDAtelierDeLaFeuilleDeTempsEntity> findBySuiviIdInAndOperateurIdAndAnnulationDateIsNullOrderByDateDeSurvenueAscIdAsc(
    Set<UUID> suivis,
    UUID operateurId
  );
}
