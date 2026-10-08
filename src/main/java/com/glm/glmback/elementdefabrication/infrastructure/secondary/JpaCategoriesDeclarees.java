package com.glm.glmback.elementdefabrication.infrastructure.secondary;

import com.glm.glmback.elementdefabrication.domain.Categorie;
import com.glm.glmback.elementdefabrication.domain.CategoriesDeclarees;
import org.springframework.stereotype.Repository;

/**
 * Les categories de produit declarees par l'entreprise courante.
 */
@Repository
class JpaCategoriesDeclarees implements CategoriesDeclarees {

  private final SpringDataCategoriesDeclareesRepository categories;

  JpaCategoriesDeclarees(SpringDataCategoriesDeclareesRepository categories) {
    this.categories = categories;
  }

  @Override
  public boolean existe(Categorie categorie) {
    return categories.findById(categorie.value()).isPresent();
  }
}
