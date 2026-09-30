package com.glm.glmback.coutderevient.domain;

import java.util.*;

/** Les ports de lecture servis depuis la memoire. */
final class AtelierEnMemoire implements ElementsValorisables, TravailDeLElement, OccupationDesOperateurs, ConflitsDuCout {

  private final Map<ElementId, ElementValorise> elements = new HashMap<>();
  private final Map<ElementId, List<ActiviteInterpretee>> travaux = new HashMap<>();
  private final List<ActiviteInterpretee> occupation = new ArrayList<>();

  AtelierEnMemoire connait(ElementValorise element) {
    elements.put(element.element(), element);
    return this;
  }

  AtelierEnMemoire aTravaille(ElementId element, ActiviteInterpretee... activites) {
    travaux.computeIfAbsent(element, ignore -> new ArrayList<>()).addAll(List.of(activites));
    occupation.addAll(List.of(activites));
    return this;
  }

  AtelierEnMemoire aMeneDeFront(ActiviteInterpretee... activites) {
    occupation.addAll(List.of(activites));
    return this;
  }

  @Override
  public Optional<ElementValorise> get(ElementId element) {
    return Optional.ofNullable(elements.get(element));
  }

  @Override
  public List<ActiviteInterpretee> activites(ElementId element) {
    return travaux.getOrDefault(element, List.of());
  }

  @Override
  public List<ActiviteInterpretee> activites(Set<OperateurId> operateurs, Periode periode) {
    return List.copyOf(occupation);
  }

  @Override
  public List<SequenceEnConflit> deLElement(ElementId element) {
    return List.of();
  }

  @Override
  public List<SequenceEnConflit> desOperateurs(Set<OperateurId> operateurs) {
    return List.of();
  }
}
