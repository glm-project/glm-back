package com.glm.glmback.coutderevient.infrastructure.secondary;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataEvenementsDAtelierValorisesRepository extends JpaRepository<EvenementDAtelierValoriseEntity, UUID> {
  /**
   * Le tri porte aussi sur l'identifiant : a horodatage egal, l'ordre du journal se departagerait au hasard, et le
   * repli du domaine, qui trie de facon stable sur la seule date de survenue, conserve celui-ci.
   */
  List<EvenementDAtelierValoriseEntity> findBySuiviIdInAndAnnulationDateIsNullOrderByDateDeSurvenueAscIdAsc(Set<UUID> suivis);

  List<EvenementDAtelierValoriseEntity> findBySuiviIdInAndOperateurIdInAndAnnulationDateIsNullOrderByDateDeSurvenueAscIdAsc(
    Set<UUID> suivis,
    Set<UUID> operateurs
  );

  /**
   * Les passages en atelier ou ces operateurs ont pointe avant la fin de l'element, servis par
   * {@code ix_evenement_d_atelier_operateur}.
   *
   * <p>
   * Aucune borne basse : une activite ouverte avant l'element et fermee apres lui le recouvre pourtant, et deux
   * bornes la feraient disparaitre du diviseur.
   * </p>
   */
  @Query(
    """
    select distinct evenement.suiviId
    from EvenementDAtelierValoriseEntity evenement
    where evenement.operateurId in :operateurs
      and evenement.dateDeSurvenue < :avant
      and evenement.annulationDate is null
    """
  )
  Set<UUID> suivisDesOperateurs(@Param("operateurs") Set<UUID> operateurs, @Param("avant") Instant avant);
}
