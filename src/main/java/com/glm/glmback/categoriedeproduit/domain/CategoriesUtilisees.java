package com.glm.glmback.categoriedeproduit.domain;

/**
 * Ce que ce contexte sait des elements de fabrication, sans jamais dependre de leur contexte.
 *
 * <p>
 * La regle qui interdit de supprimer une categorie portee par un element vit ainsi dans le domaine.
 * </p>
 */
public interface CategoriesUtilisees {
  boolean estUtilisee(CodeDeCategorie code);
}
