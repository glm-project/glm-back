package com.glm.glmback.categoriedeproduit.infrastructure.secondary;

import com.glm.glmback.categoriedeproduit.domain.CategoriesUtilisees;
import com.glm.glmback.categoriedeproduit.domain.CodeDeCategorie;
import org.springframework.stereotype.Repository;

/**
 * Ce qui empeche de supprimer une categorie : les elements de fabrication de l'entreprise courante qui y sont ranges.
 */
@Repository
class ElementsDesCategories implements CategoriesUtilisees {

  private final SpringDataElementsCategorisesRepository elements;

  ElementsDesCategories(SpringDataElementsCategorisesRepository elements) {
    this.elements = elements;
  }

  @Override
  public boolean estUtilisee(CodeDeCategorie code) {
    return elements.findFirstByCategorie(code.value()).isPresent();
  }
}
