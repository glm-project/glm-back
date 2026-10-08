package com.glm.glmback.coutderevient.domain;

import java.util.*;

/** Les ports de lecture servis depuis la memoire. */
final class AtelierEnMemoire implements ElementsValorisables, TravailDeLElement, OccupationDesOperateurs, OperateursNommes, PostesNommes {

  private final Map<ElementId, ElementValorise> elements = new HashMap<>();
  private final Map<ElementId, List<ActiviteInterpretee>> travaux = new HashMap<>();
  private final List<ActiviteInterpretee> occupation = new ArrayList<>();
  private final List<OperateurNomme> operateurs = new ArrayList<>();
  private final List<PosteNomme> postes = new ArrayList<>();

  AtelierEnMemoire connait(ElementValorise element) {
    elements.put(element.element(), element);
    return this;
  }

  AtelierEnMemoire nomme(OperateurNomme operateur) {
    operateurs.add(operateur);
    return this;
  }

  AtelierEnMemoire nomme(PosteNomme poste) {
    postes.add(poste);
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
  public List<ElementValorise> tous(Set<ElementId> ids) {
    return ids
      .stream()
      .flatMap(id -> Optional.ofNullable(elements.get(id)).stream())
      .toList();
  }

  @Override
  public List<OperateurNomme> operateurs(Set<OperateurId> ids) {
    return operateurs
      .stream()
      .filter(operateur -> ids.contains(operateur.operateur()))
      .toList();
  }

  @Override
  public List<PosteNomme> postes(Set<PosteDeTravailId> ids) {
    return postes
      .stream()
      .filter(poste -> ids.contains(poste.poste()))
      .toList();
  }

  @Override
  public List<ActiviteInterpretee> activites(ElementId element) {
    return travaux.getOrDefault(element, List.of());
  }

  @Override
  public List<ActiviteInterpretee> activites(Set<OperateurId> operateurs, Periode periode) {
    return List.copyOf(occupation);
  }
}
