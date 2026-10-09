package com.glm.glmback.naturedetravail.domain;

import java.util.Collection;
import java.util.Set;

/**
 * Ce que ce contexte sait de ceux qui se servent des natures — postes et pointages — sans jamais dependre de leurs
 * contextes.
 */
public interface NaturesEnUsage {
  boolean estUtilisee(NatureDeTravailId nature);

  /**
   * Les natures, parmi celles donnees, qui servent deja : une seule lecture pour toute une page.
   */
  Set<NatureDeTravailId> utiliseesParmi(Collection<NatureDeTravailId> natures);
}
