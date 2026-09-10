package com.glm.glmback.coutderevient.infrastructure.secondary;

import com.glm.glmback.coutderevient.domain.EvenementDePresence;
import com.glm.glmback.coutderevient.domain.JourneeDeTravail;
import com.glm.glmback.coutderevient.domain.OperateurId;
import com.glm.glmback.coutderevient.domain.Periode;
import com.glm.glmback.coutderevient.domain.PresenceDUnOperateur;
import com.glm.glmback.coutderevient.domain.PresenceDesOperateurs;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

/**
 * La presence lue dans les tables de l'atelier, sans jamais importer son code.
 *
 * <p>
 * Deux requetes, jamais une par journee : les journees qui recouvrent la periode du rapport d'abord, puis leurs
 * journaux d'un seul coup.
 * </p>
 */
@Repository
class PresenceValorisee implements PresenceDesOperateurs {

  private final SpringDataJourneesValoriseesRepository journees;
  private final SpringDataEvenementsDePresenceValorisesRepository evenements;

  PresenceValorisee(SpringDataJourneesValoriseesRepository journees, SpringDataEvenementsDePresenceValorisesRepository evenements) {
    this.journees = journees;
    this.evenements = evenements;
  }

  @Override
  public List<PresenceDUnOperateur> presences(Set<OperateurId> operateurs, Periode periode) {
    List<JourneeDeTravailValoriseeEntity> venues = journees.recouvrant(uuids(operateurs), periode.debut(), periode.fin());
    Map<UUID, List<EvenementDePresence>> parJournee = journaux(venues);

    return venues
      .stream()
      .collect(
        Collectors.groupingBy(
          JourneeDeTravailValoriseeEntity::operateurId,
          Collectors.mapping(venue -> new JourneeDeTravail(parJournee.getOrDefault(venue.id(), List.of())), Collectors.toList())
        )
      )
      .entrySet()
      .stream()
      .map(entree -> new PresenceDUnOperateur(new OperateurId(entree.getKey()), entree.getValue()))
      .toList();
  }

  private Map<UUID, List<EvenementDePresence>> journaux(List<JourneeDeTravailValoriseeEntity> venues) {
    return evenements
      .findByJourneeIdInAndAnnulationDateIsNullOrderByDateDeSurvenueAscIdAsc(identifiants(venues))
      .stream()
      .collect(
        Collectors.groupingBy(
          EvenementDePresenceValoriseEntity::journeeId,
          Collectors.mapping(EvenementDePresenceValoriseEntity::toDomain, Collectors.<EvenementDePresence>toList())
        )
      );
  }

  private static Set<UUID> identifiants(List<JourneeDeTravailValoriseeEntity> venues) {
    return venues.stream().map(JourneeDeTravailValoriseeEntity::id).collect(Collectors.toSet());
  }

  private static Set<UUID> uuids(Set<OperateurId> operateurs) {
    return operateurs.stream().map(OperateurId::uuid).collect(Collectors.toSet());
  }
}
