package com.glm.glmback.feuilledetemps.infrastructure.secondary;

import com.glm.glmback.feuilledetemps.domain.EvenementDePresence;
import com.glm.glmback.feuilledetemps.domain.JourneeDeTravail;
import com.glm.glmback.feuilledetemps.domain.OperateurId;
import com.glm.glmback.feuilledetemps.domain.PresenceDeLOperateur;
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
class JourneesDeTravailDAtelier implements PresenceDeLOperateur {

  private final SpringDataJourneesDeTravailLectureRepository journees;
  private final SpringDataEvenementsDePresenceLectureRepository evenements;

  JourneesDeTravailDAtelier(
    SpringDataJourneesDeTravailLectureRepository journees,
    SpringDataEvenementsDePresenceLectureRepository evenements
  ) {
    this.journees = journees;
    this.evenements = evenements;
  }

  @Override
  public List<JourneeDeTravail> journeesRecouvrant(OperateurId operateur, Instant debut, Instant finExclusive) {
    Set<UUID> recouvrantes = journees
      .recouvrant(operateur.uuid(), debut, finExclusive)
      .stream()
      .map(JourneeDeTravailLectureEntity::id)
      .collect(Collectors.toSet());

    return evenements
      .findByJourneeIdInAndAnnulationDateIsNullOrderByDateDeSurvenueAsc(recouvrantes)
      .stream()
      .collect(
        Collectors.groupingBy(
          EvenementDePresenceLectureEntity::journeeId,
          LinkedHashMap::new,
          Collectors.mapping(EvenementDePresenceLectureEntity::toDomain, Collectors.<EvenementDePresence>toList())
        )
      )
      .values()
      .stream()
      .map(JourneeDeTravail::new)
      .toList();
  }
}
