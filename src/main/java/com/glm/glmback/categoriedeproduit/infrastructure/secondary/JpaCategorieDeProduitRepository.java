package com.glm.glmback.categoriedeproduit.infrastructure.secondary;

import com.glm.glmback.categoriedeproduit.domain.CategorieDeProduit;
import com.glm.glmback.categoriedeproduit.domain.CategorieDeProduitRepository;
import com.glm.glmback.categoriedeproduit.domain.CategorieDejaExistanteException;
import com.glm.glmback.categoriedeproduit.domain.CategorieIntrouvableException;
import com.glm.glmback.categoriedeproduit.domain.CodeDeCategorie;
import com.glm.glmback.categoriedeproduit.domain.Rang;
import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
class JpaCategorieDeProduitRepository implements CategorieDeProduitRepository {

  private static final Sort PAR_RANG = Sort.by(Sort.Order.asc("rang"), Sort.Order.asc("code"));

  private final SpringDataCategorieDeProduitRepository categories;

  JpaCategorieDeProduitRepository(SpringDataCategorieDeProduitRepository categories) {
    this.categories = categories;
  }

  @Override
  public CategorieDeProduit create(CategorieDeProduit categorie) {
    if (categories.existsById(categorie.code().value())) {
      throw new CategorieDejaExistanteException(categorie.code());
    }
    categories.save(CategorieDeProduitEntity.from(categorie));

    return categorie;
  }

  @Override
  public CategorieDeProduit update(CategorieDeProduit categorie) {
    if (!categories.existsById(categorie.code().value())) {
      throw new CategorieIntrouvableException(categorie.code());
    }
    categories.save(CategorieDeProduitEntity.from(categorie));

    return categorie;
  }

  @Override
  public void delete(CodeDeCategorie code) {
    if (!categories.existsById(code.value())) {
      throw new CategorieIntrouvableException(code);
    }
    categories.deleteById(code.value());
  }

  @Override
  public Optional<CategorieDeProduit> get(CodeDeCategorie code) {
    return categories.findById(code.value()).map(CategorieDeProduitEntity::toDomain);
  }

  @Override
  public Optional<Rang> dernierRang() {
    return categories.findDernierRang().map(Rang::new);
  }

  @Override
  public long compte() {
    return categories.count();
  }

  @Override
  public Page<CategorieDeProduit> list(Pageable pageable) {
    var page = categories.findAll(PageRequest.of(pageable.page(), pageable.size(), PAR_RANG));

    return Page.<CategorieDeProduit>builder()
      .content(page.getContent().stream().map(CategorieDeProduitEntity::toDomain).toList())
      .currentPage(pageable.page())
      .pageSize(pageable.size())
      .totalElementsCount(page.getTotalElements());
  }
}
