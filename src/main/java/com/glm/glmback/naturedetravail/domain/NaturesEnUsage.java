package com.glm.glmback.naturedetravail.domain;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Ce que ce contexte sait de ceux qui se servent des natures — postes et pointages — sans jamais dependre de leurs
 * contextes.
 */
public interface NaturesEnUsage {
  /**
   * Un poste porte la nature.
   */
  boolean estUtilisee(NatureDeTravailId nature);

  /**
   * Un pointage a recopie la nature : c'est definitif.
   */
  boolean estPointee(NatureDeTravailId nature);

  /**
   * Les natures, parmi celles donnees, qu'un poste porte ou qu'un pointage a recopiees : une seule lecture pour toute
   * une page.
   */
  Set<NatureDeTravailId> utiliseesParmi(Collection<NatureDeTravailId> natures);

  /**
   * Le nombre de postes qui portent chacune des natures donnees ; une nature qu'aucun poste ne porte est absente.
   */
  Map<NatureDeTravailId, Integer> postesParmi(Collection<NatureDeTravailId> natures);
}
