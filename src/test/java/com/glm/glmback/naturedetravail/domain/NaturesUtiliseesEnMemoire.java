package com.glm.glmback.naturedetravail.domain;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Doublure de test du port d'usage : elle tient lieu des postes et des pointages, que ce contexte ne connait que par la
 * donnee.
 */
final class NaturesUtiliseesEnMemoire implements NaturesEnUsage {

  private final Map<NatureDeTravailId, Integer> postes = new HashMap<>();
  private final Set<NatureDeTravailId> pointees = new HashSet<>();

  /**
   * Un poste de plus porte la nature.
   */
  void utilise(NatureDeTravailId id) {
    postes.merge(id, 1, Integer::sum);
  }

  void pointe(NatureDeTravailId id) {
    pointees.add(id);
  }

  @Override
  public boolean estPointee(NatureDeTravailId nature) {
    return pointees.contains(nature);
  }

  @Override
  public boolean estUtilisee(NatureDeTravailId nature) {
    return postes.containsKey(nature);
  }

  @Override
  public Set<NatureDeTravailId> utiliseesParmi(Collection<NatureDeTravailId> natures) {
    return natures
      .stream()
      .filter(nature -> postes.containsKey(nature) || pointees.contains(nature))
      .collect(Collectors.toUnmodifiableSet());
  }

  @Override
  public Map<NatureDeTravailId, Integer> postesParmi(Collection<NatureDeTravailId> natures) {
    return natures.stream().filter(postes::containsKey).collect(Collectors.toUnmodifiableMap(Function.identity(), postes::get));
  }
}
