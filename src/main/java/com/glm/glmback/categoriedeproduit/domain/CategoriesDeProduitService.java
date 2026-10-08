package com.glm.glmback.categoriedeproduit.domain;

import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;

public final class CategoriesDeProduitService {

  private final CategorieDeProduitRepository repository;

  public CategoriesDeProduitService(CategorieDeProduitRepository repository) {
    this.repository = repository;
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
}
