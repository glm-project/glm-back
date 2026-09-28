package com.glm.glmback.feuilledetemps.infrastructure.secondary;

import com.glm.glmback.feuilledetemps.domain.ElementId;
import com.glm.glmback.feuilledetemps.domain.JournalDAtelier;
import com.glm.glmback.feuilledetemps.domain.OperateurId;
import com.glm.glmback.feuilledetemps.domain.PointageDAtelier;
import com.glm.glmback.feuilledetemps.domain.SuiviDuTravail;
import com.glm.glmback.feuilledetemps.domain.TravailDeLOperateur;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

/**
 * Le travail d'un operateur lu dans les tables de l'atelier, sans jamais importer son code.
 *
 * <p>
 * Trois requetes, jamais une par suivi : les suivis ou l'operateur a pointe sur la periode, ces suivis, puis leurs
 * journaux d'un seul coup, restreints a l'operateur. Les evenements annules sont ecartes des le SQL.
 * </p>
 */
@Repository
class TravailDeLaFeuilleDeTemps implements TravailDeLOperateur {

  private final SpringDataSuivisDeLaFeuilleDeTempsRepository suivis;
  private final SpringDataPointagesDAtelierDeLaFeuilleDeTempsRepository pointages;

  TravailDeLaFeuilleDeTemps(
    SpringDataSuivisDeLaFeuilleDeTempsRepository suivis,
    SpringDataPointagesDAtelierDeLaFeuilleDeTempsRepository pointages
  ) {
    this.suivis = suivis;
    this.pointages = pointages;
  }

  @Override
  public List<SuiviDuTravail> suivis(OperateurId operateur, Instant depuis, Instant finExclusive) {
    Set<UUID> passages = pointages.suivisDeLOperateur(operateur.uuid(), depuis, finExclusive);
    Map<UUID, List<PointageDAtelier>> parSuivi = pointages
      .findBySuiviIdInAndOperateurIdAndAnnulationDateIsNullOrderByDateDeSurvenueAscIdAsc(passages, operateur.uuid())
      .stream()
      .collect(
        Collectors.groupingBy(
          PointageDAtelierDeLaFeuilleDeTempsEntity::suiviId,
          Collectors.mapping(PointageDAtelierDeLaFeuilleDeTempsEntity::toDomain, Collectors.<PointageDAtelier>toList())
        )
      );

    return suivis
      .findByIdIn(passages)
      .stream()
      .map(passage ->
        new SuiviDuTravail(new ElementId(passage.elementId()), new JournalDAtelier(parSuivi.get(passage.id())), passage.cloture())
      )
      .toList();
  }
}
