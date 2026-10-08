package com.glm.glmback.categoriedeproduit.application;

import com.glm.glmback.categoriedeproduit.domain.CategorieDeProduit;
import com.glm.glmback.categoriedeproduit.domain.CategorieDeProduitRepository;
import com.glm.glmback.categoriedeproduit.domain.CategoriesDeProduitService;
import com.glm.glmback.categoriedeproduit.domain.CodeDeCategorie;
import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoriesDeProduitApplicationService {

  private final CategoriesDeProduitService categories;

  public CategoriesDeProduitApplicationService(CategorieDeProduitRepository repository) {
    this.categories = new CategoriesDeProduitService(repository);
  }

  @Secured("ROLE_GESTIONNAIRE")
  @Transactional
  public CategorieDeProduit create(CodeDeCategorie code) {
    return categories.create(code);
  }

  @Secured({ "ROLE_USER", "ROLE_GESTIONNAIRE" })
  @Transactional(readOnly = true)
  public Page<CategorieDeProduit> list(Pageable pageable) {
    return categories.list(pageable);
  }
}
