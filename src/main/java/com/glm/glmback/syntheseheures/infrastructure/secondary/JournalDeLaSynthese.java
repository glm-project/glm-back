package com.glm.glmback.syntheseheures.infrastructure.secondary;

import com.glm.glmback.syntheseheures.domain.JournalDElement;
import com.glm.glmback.syntheseheures.domain.JournalDeLOperateur;
import com.glm.glmback.syntheseheures.domain.OperateurId;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

/** Tous les pointages de la periode, meme lorsqu'ils ne laissent aucun travail. */
@Repository
class JournalDeLaSynthese implements JournalDeLOperateur {

  private final SpringDataSuivisDeLaSyntheseRepository suivis;
  private final SpringDataPointagesDAtelierDeLaSyntheseRepository pointages;

  JournalDeLaSynthese(SpringDataSuivisDeLaSyntheseRepository suivis, SpringDataPointagesDAtelierDeLaSyntheseRepository pointages) {
    this.suivis = suivis;
    this.pointages = pointages;
  }

  @Override
  public List<JournalDElement> dans(OperateurId operateur, Instant debut, Instant finExclusive) {
    var parSuivi = pointages
      .findByOperateurIdAndDateDeSurvenueGreaterThanEqualAndDateDeSurvenueLessThanOrderByDateDeSurvenueAscIdAsc(
        operateur.uuid(),
        debut,
        finExclusive
      )
      .stream()
      .collect(Collectors.groupingBy(PointageDAtelierDeLaSyntheseEntity::suiviId));
    return suivis
      .findByIdIn(parSuivi.keySet())
      .stream()
      .map(suivi ->
        new JournalDElement(
          suivi.element(),
          parSuivi
            .get(suivi.id())
            .stream()
            .map(pointage -> pointage.toDomain(suivi.element().id()))
            .toList()
        )
      )
      .toList();
  }
}
