package com.glm.glmback.pupitre.infrastructure.secondary;

import com.glm.glmback.pupitre.domain.CategorieDElement;
import com.glm.glmback.pupitre.domain.CategoriesDuPupitre;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * Les categories de produit de l'entreprise courante, rangees par rang puis par code.
 *
 * <p>
 * Le code departage deux rangs egaux, comme dans le referentiel des categories : le pupitre et la gestion montrent le
 * meme ordre.
 * </p>
 */
@Repository
class CategoriesDuReferentielDuPupitre implements CategoriesDuPupitre {

  private final SpringDataCategoriesDuPupitreRepository categories;

  CategoriesDuReferentielDuPupitre(SpringDataCategoriesDuPupitreRepository categories) {
    this.categories = categories;
  }

  @Override
  public List<CategorieDElement> toutes() {
    return categories.toutes().stream().map(CategorieDuPupitreEntity::toDomain).toList();
  }
}
