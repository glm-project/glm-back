package com.glm.glmback.syntheseheures.infrastructure.secondary;

import com.glm.glmback.syntheseheures.domain.JournalDAtelier;
import com.glm.glmback.syntheseheures.domain.OperateurId;
import com.glm.glmback.syntheseheures.domain.PointageDAtelier;
import com.glm.glmback.syntheseheures.domain.SuiviDuTravail;
import com.glm.glmback.syntheseheures.domain.TravailDeLOperateur;
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
class TravailDeLaSynthese implements TravailDeLOperateur {

  private final SpringDataSuivisDeLaSyntheseRepository suivis;
  private final SpringDataPointagesDAtelierDeLaSyntheseRepository pointages;

  TravailDeLaSynthese(SpringDataSuivisDeLaSyntheseRepository suivis, SpringDataPointagesDAtelierDeLaSyntheseRepository pointages) {
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
          PointageDAtelierDeLaSyntheseEntity::suiviId,
          Collectors.mapping(PointageDAtelierDeLaSyntheseEntity::toDomain, Collectors.<PointageDAtelier>toList())
        )
      );

    return suivis
      .findByIdIn(passages)
      .stream()
      .map(passage -> new SuiviDuTravail(passage.element(), new JournalDAtelier(parSuivi.get(passage.id())), passage.cloture()))
      .toList();
  }
}
