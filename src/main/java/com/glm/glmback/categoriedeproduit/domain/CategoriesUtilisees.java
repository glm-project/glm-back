package com.glm.glmback.categoriedeproduit.domain;

import java.util.Collection;
import java.util.Set;

/**
 * Ce que ce contexte sait des elements de fabrication, sans jamais dependre de leur contexte.
 *
 * <p>
 * La regle qui interdit de supprimer une categorie portee par un element vit ainsi dans le domaine.
 * </p>
 */
public interface CategoriesUtilisees {
  boolean estUtilisee(CodeDeCategorie code);

  /**
   * Les codes, parmi ceux donnes, sous lesquels au moins un element est range : une seule lecture pour toute une page de
   * categories.
   */
  Set<CodeDeCategorie> utiliseesParmi(Collection<CodeDeCategorie> codes);
}
