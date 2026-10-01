package com.glm.glmback.coutderevient.infrastructure.secondary;

import com.glm.glmback.coutderevient.domain.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

/** Sequences et identites de leurs activites lues par lots. */
@Repository
class ConflitsValorises implements ConflitsDuCout {

  private final SpringDataConflitsDuCoutRepository conflits;
  private final SpringDataActivitesValoriseesRepository activites;

  ConflitsValorises(SpringDataConflitsDuCoutRepository conflits, SpringDataActivitesValoriseesRepository activites) {
    this.conflits = conflits;
    this.activites = activites;
  }

  @Override
  public List<SequenceEnConflit> deLElement(ElementId element) {
    return sequences(conflits.deLElement(element.uuid()));
  }

  @Override
  public List<SequenceEnConflit> desOperateurs(Set<OperateurId> operateurs) {
    return sequences(conflits.desOperateurs(operateurs.stream().map(OperateurId::uuid).collect(Collectors.toSet())));
  }

  private List<SequenceEnConflit> sequences(List<SequenceEnConflitDuCoutEntity> lues) {
    if (lues.isEmpty()) {
      return List.of();
    }
    Set<UUID> ids = lues.stream().map(SequenceEnConflitDuCoutEntity::id).collect(Collectors.toSet());
    Map<UUID, List<ActiviteId>> parSequence = activites
      .dansConflits(ids)
      .stream()
      .collect(
        Collectors.groupingBy(
          ActiviteValoriseeEntity::sequenceId,
          Collectors.mapping(ActiviteValoriseeEntity::identite, Collectors.toList())
        )
      );
    return lues
      .stream()
      .map(sequence -> sequence.toDomain(parSequence.getOrDefault(sequence.id(), List.of())))
      .toList();
  }
}
