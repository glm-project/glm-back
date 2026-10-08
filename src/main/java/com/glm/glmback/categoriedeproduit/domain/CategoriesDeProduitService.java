package com.glm.glmback.categoriedeproduit.domain;

import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;

public final class CategoriesDeProduitService {

  private final CategorieDeProduitRepository repository;
  private final CategoriesUtilisees usages;

  public CategoriesDeProduitService(CategorieDeProduitRepository repository, CategoriesUtilisees usages) {
    this.repository = repository;
    this.usages = usages;
  }

  /**
   * Une categorie nouvelle se range en dernier : l'ordre deja choisi par l'entreprise ne bouge pas.
   */
  public CategorieDeProduit create(CodeDeCategorie code) {
    if (repository.get(code).isPresent()) {
      throw new CategorieDejaExistanteException(code);
    }

    return repository.create(new CategorieDeProduit(code, repository.dernierRang().map(Rang::suivant).orElseGet(Rang::premier)));
  }

  public Page<CategorieDeProduit> list(Pageable pageable) {
    return repository.list(pageable);
  }

  /**
   * Une categorie qui range deja des produits ne se supprime pas : leur nom porte son code, et ils resteraient ranges
   * dans une famille disparue.
   */
  public void delete(CodeDeCategorie code) {
    if (repository.get(code).isEmpty()) {
      throw new CategorieIntrouvableException(code);
    }
    if (usages.estUtilisee(code)) {
      throw new CategorieUtiliseeException(code);
    }
    repository.delete(code);
  }
}
