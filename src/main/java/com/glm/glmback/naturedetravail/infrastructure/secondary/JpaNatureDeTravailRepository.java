package com.glm.glmback.naturedetravail.infrastructure.secondary;

import com.glm.glmback.naturedetravail.domain.CleDeNature;
import com.glm.glmback.naturedetravail.domain.NatureDeTravail;
import com.glm.glmback.naturedetravail.domain.NatureDeTravailDejaCreeeException;
import com.glm.glmback.naturedetravail.domain.NatureDeTravailId;
import com.glm.glmback.naturedetravail.domain.NatureDeTravailRepository;
import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
class JpaNatureDeTravailRepository implements NatureDeTravailRepository {

  private static final Sort PAR_CLE = Sort.by(Sort.Order.asc("cle"), Sort.Order.asc("id"));

  private final SpringDataNatureDeTravailRepository natures;

  JpaNatureDeTravailRepository(SpringDataNatureDeTravailRepository natures) {
    this.natures = natures;
  }

  @Override
  public NatureDeTravail create(NatureDeTravail nature) {
    if (natures.existsById(nature.id().uuid())) {
      throw new NatureDeTravailDejaCreeeException(nature.id());
    }
    natures.save(NatureDeTravailEntity.from(nature));

    return nature;
  }

  @Override
  public Optional<NatureDeTravail> get(NatureDeTravailId id) {
    return natures.findById(id.uuid()).map(NatureDeTravailEntity::toDomain);
  }

  @Override
  public Optional<NatureDeTravailId> idPourCle(CleDeNature cle) {
    return natures.findIdByCle(cle.value()).map(NatureDeTravailId::new);
  }

  @Override
  public Page<NatureDeTravail> list(Pageable pageable) {
    var page = natures.findAll(PageRequest.of(pageable.page(), pageable.size(), PAR_CLE));

    return Page.<NatureDeTravail>builder()
      .content(page.getContent().stream().map(NatureDeTravailEntity::toDomain).toList())
      .currentPage(pageable.page())
      .pageSize(pageable.size())
      .totalElementsCount(page.getTotalElements());
  }
}
