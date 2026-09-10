package com.glm.glmback.syntheseheures.infrastructure.secondary;

import com.glm.glmback.syntheseheures.domain.EvenementDePresence;
import com.glm.glmback.syntheseheures.domain.JourneeDeTravail;
import com.glm.glmback.syntheseheures.domain.OperateurId;
import com.glm.glmback.syntheseheures.domain.PresenceDeLOperateur;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

/**
 * La presence lue dans les tables de l'atelier, sans jamais importer son code.
 *
 * <p>
 * Deux requetes, jamais une par journee : les journees qui recouvrent la semaine d'abord, puis leurs journaux d'un
 * seul coup. Les evenements annules sont ecartes des le SQL — les rapporter pour les filtrer ensuite ferait porter au
 * domaine une correction qui ne le regarde pas.
 * </p>
 */
@Repository
class JourneesDeTravailPourLaSynthese implements PresenceDeLOperateur {

  private final SpringDataJourneesDeTravailSyntheseRepository journees;
  private final SpringDataEvenementsDePresenceSyntheseRepository evenements;

  JourneesDeTravailPourLaSynthese(
    SpringDataJourneesDeTravailSyntheseRepository journees,
    SpringDataEvenementsDePresenceSyntheseRepository evenements
  ) {
    this.journees = journees;
    this.evenements = evenements;
  }

  @Override
  public List<JourneeDeTravail> journeesRecouvrant(OperateurId operateur, Instant debut, Instant finExclusive) {
    Set<UUID> recouvrantes = journees
      .recouvrant(operateur.uuid(), debut, finExclusive)
      .stream()
      .map(JourneeDeTravailSyntheseEntity::id)
      .collect(Collectors.toSet());

    return evenements
      .findByJourneeIdInAndAnnulationDateIsNullOrderByDateDeSurvenueAsc(recouvrantes)
      .stream()
      .collect(
        Collectors.groupingBy(
          EvenementDePresenceSyntheseEntity::journeeId,
          LinkedHashMap::new,
          Collectors.mapping(EvenementDePresenceSyntheseEntity::toDomain, Collectors.<EvenementDePresence>toList())
        )
      )
      .values()
      .stream()
      .map(JourneeDeTravail::new)
      .toList();
  }
}
