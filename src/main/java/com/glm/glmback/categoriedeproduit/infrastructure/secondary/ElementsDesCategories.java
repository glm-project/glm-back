package com.glm.glmback.categoriedeproduit.infrastructure.secondary;

import com.glm.glmback.categoriedeproduit.domain.CategoriesUtilisees;
import com.glm.glmback.categoriedeproduit.domain.CodeDeCategorie;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
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

  @Override
  public Set<CodeDeCategorie> utiliseesParmi(Collection<CodeDeCategorie> codes) {
    if (codes.isEmpty()) {
      return Set.of();
    }

    return elements
      .findCategoriesUtiliseesParmi(codes.stream().map(CodeDeCategorie::value).toList())
      .stream()
      .map(CodeDeCategorie::new)
      .collect(Collectors.toUnmodifiableSet());
  }
}
