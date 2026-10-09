package com.glm.glmback.naturedetravail.domain;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Doublure de test du port d'usage : elle tient lieu des postes et des pointages, que ce contexte ne connait que par la
 * donnee.
 */
final class NaturesUtiliseesEnMemoire implements NaturesEnUsage {

  private final Set<NatureDeTravailId> utilisees = new HashSet<>();

  void utilise(NatureDeTravailId id) {
    utilisees.add(id);
  }

  @Override
  public boolean estUtilisee(NatureDeTravailId nature) {
    return utilisees.contains(nature);
  }

  @Override
  public Set<NatureDeTravailId> utiliseesParmi(Collection<NatureDeTravailId> natures) {
    return natures.stream().filter(utilisees::contains).collect(Collectors.toUnmodifiableSet());
  }
}
