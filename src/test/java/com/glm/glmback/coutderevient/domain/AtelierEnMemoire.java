package com.glm.glmback.coutderevient.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Les quatre ports du contexte, servis depuis la memoire.
 *
 * <p>
 * Le double retient l'instant demande a la lecture d'occupation : c'est ce qui permet de verifier que le service
 * borne bien sa requete sur la fin de l'element, sans quoi il ramenerait tout l'historique de l'atelier.
 * </p>
 */
final class AtelierEnMemoire implements ElementsValorisables, TravailDeLElement, OccupationDesOperateurs, PresenceDesOperateurs {

  private final Map<ElementId, ElementValorise> elements = new java.util.HashMap<>();
  private final Map<ElementId, List<SuiviDuTravail>> travaux = new java.util.HashMap<>();
  private final List<SuiviDuTravail> occupation = new ArrayList<>();
  private final List<PresenceDUnOperateur> presences = new ArrayList<>();
  private Instant borneDemandee;

  AtelierEnMemoire connait(ElementValorise element) {
    elements.put(element.element(), element);

    return this;
  }

  /**
   * Un suivi de l'element compte aussi dans l'occupation de ses operateurs : c'est ce que fait la base, ou les deux
   * lectures portent sur la meme table.
   */
  AtelierEnMemoire aTravaille(ElementId element, SuiviDuTravail suivi) {
    travaux.computeIfAbsent(element, ignore -> new ArrayList<>()).add(suivi);
    occupation.add(suivi);

    return this;
  }

  AtelierEnMemoire aMeneDeFront(SuiviDuTravail suivi) {
    occupation.add(suivi);

    return this;
  }

  AtelierEnMemoire aEtePresent(PresenceDUnOperateur presence) {
    presences.add(presence);

    return this;
  }

  @Override
  public Optional<ElementValorise> get(ElementId element) {
    return Optional.ofNullable(elements.get(element));
  }

  @Override
  public List<SuiviDuTravail> suivis(ElementId element) {
    return travaux.getOrDefault(element, List.of());
  }

  @Override
  public List<SuiviDuTravail> suivis(Set<OperateurId> operateurs, Instant avant) {
    borneDemandee = avant;

    return List.copyOf(occupation);
  }

  @Override
  public List<PresenceDUnOperateur> presences(Set<OperateurId> operateurs, Periode periode) {
    return presences
      .stream()
      .filter(presence -> operateurs.contains(presence.operateur()))
      .collect(Collectors.toList());
  }

  Instant borneDemandee() {
    return borneDemandee;
  }
}
