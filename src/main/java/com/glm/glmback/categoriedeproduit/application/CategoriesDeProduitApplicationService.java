package com.glm.glmback.categoriedeproduit.application;

import com.glm.glmback.categoriedeproduit.domain.CategorieDeProduit;
import com.glm.glmback.categoriedeproduit.domain.CategorieDeProduitRepository;
import com.glm.glmback.categoriedeproduit.domain.CategoriesDeProduitService;
import com.glm.glmback.categoriedeproduit.domain.CategoriesUtilisees;
import com.glm.glmback.categoriedeproduit.domain.CodeDeCategorie;
import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import java.util.List;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoriesDeProduitApplicationService {

  private final CategoriesDeProduitService categories;

  public CategoriesDeProduitApplicationService(CategorieDeProduitRepository repository, CategoriesUtilisees usages) {
    this.categories = new CategoriesDeProduitService(repository, usages);
  }

  @Secured("ROLE_GESTIONNAIRE")
  @Transactional
  public CategorieDeProduit create(CodeDeCategorie code) {
    return categories.create(code);
  }

  @Secured("ROLE_GESTIONNAIRE")
  @Transactional
  public void reordonne(List<CodeDeCategorie> ordre) {
    categories.reordonne(ordre);
  }

  @Secured({ "ROLE_USER", "ROLE_GESTIONNAIRE" })
  @Transactional(readOnly = true)
  public Page<CategorieDeProduit> list(Pageable pageable) {
    return categories.list(pageable);
  }

  @Secured("ROLE_GESTIONNAIRE")
  @Transactional
  public void delete(CodeDeCategorie code) {
    categories.delete(code);
  }
}
