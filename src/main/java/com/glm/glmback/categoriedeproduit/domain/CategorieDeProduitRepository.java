package com.glm.glmback.categoriedeproduit.domain;

import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import java.util.Optional;

public interface CategorieDeProduitRepository {
  CategorieDeProduit create(CategorieDeProduit categorie);

  void delete(CodeDeCategorie code);

  Optional<CategorieDeProduit> get(CodeDeCategorie code);

  Optional<Rang> dernierRang();

  Page<CategorieDeProduit> list(Pageable pageable);
}
