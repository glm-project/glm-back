package com.glm.glmback.syntheseheures.infrastructure.secondary;

import com.glm.glmback.syntheseheures.domain.ActiviteId;
import com.glm.glmback.syntheseheures.domain.ConflitsDeLOperateur;
import com.glm.glmback.syntheseheures.domain.OperateurId;
import com.glm.glmback.syntheseheures.domain.SequenceEnConflit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

/** Deux lectures groupees de la projection, sans borne arbitraire sur le debut d'une activite. */
@Repository
class ConflitsDeLaSynthese implements ConflitsDeLOperateur {

  private final SpringDataConflitsDeLaSyntheseRepository conflits;
  private final SpringDataActivitesDeLaSyntheseRepository activites;

  ConflitsDeLaSynthese(SpringDataConflitsDeLaSyntheseRepository conflits, SpringDataActivitesDeLaSyntheseRepository activites) {
    this.conflits = conflits;
    this.activites = activites;
  }

  @Override
  public List<SequenceEnConflit> de(OperateurId operateur) {
    Map<UUID, List<ActiviteId>> parSequence = activites
      .enConflitDe(operateur.uuid())
      .stream()
      .collect(
        Collectors.groupingBy(
          ActiviteDeLaSyntheseEntity::sequenceId,
          Collectors.mapping(ActiviteDeLaSyntheseEntity::identite, Collectors.toList())
        )
      );
    return conflits
      .de(operateur.uuid())
      .stream()
      .map(sequence -> sequence.toDomain(parSequence.getOrDefault(sequence.id(), List.of())))
      .toList();
  }
}
