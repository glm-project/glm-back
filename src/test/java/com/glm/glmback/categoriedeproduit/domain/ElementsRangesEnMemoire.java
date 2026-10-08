package com.glm.glmback.categoriedeproduit.domain;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Doublure de test du port d'usage : elle tient lieu du contexte des elements de fabrication, que ce contexte ne
 * connait que par la donnee.
 */
final class ElementsRangesEnMemoire implements CategoriesUtilisees {

  private final Set<CodeDeCategorie> utilisees = new HashSet<>();

  void range(CodeDeCategorie code) {
    utilisees.add(code);
  }

  @Override
  public boolean estUtilisee(CodeDeCategorie code) {
    return utilisees.contains(code);
  }

  @Override
  public Set<CodeDeCategorie> utiliseesParmi(Collection<CodeDeCategorie> codes) {
    return codes.stream().filter(utilisees::contains).collect(Collectors.toUnmodifiableSet());
  }
}
