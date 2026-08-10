package com.glm.glmback.elementdefabrication.infrastructure.secondary;

import com.glm.glmback.elementdefabrication.domain.ElementDeFabrication;
import com.glm.glmback.elementdefabrication.domain.ElementDeFabricationCriteria;
import com.glm.glmback.elementdefabrication.domain.ElementDeFabricationDejaExistantException;
import com.glm.glmback.elementdefabrication.domain.ElementDeFabricationId;
import com.glm.glmback.elementdefabrication.domain.ElementDeFabricationIntrouvableException;
import com.glm.glmback.elementdefabrication.domain.ElementDeFabricationRepository;
import com.glm.glmback.elementdefabrication.domain.Periode;
import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
class JpaElementDeFabricationRepository implements ElementDeFabricationRepository {

  private static final Sort PAR_DATE_DE_CREATION_DESCENDANTE = Sort.by(Sort.Order.desc("dateDeCreation"), Sort.Order.asc("id"));

  private final SpringDataElementDeFabricationRepository elements;

  JpaElementDeFabricationRepository(SpringDataElementDeFabricationRepository elements) {
    this.elements = elements;
  }

  @Override
  public ElementDeFabrication create(ElementDeFabrication element) {
    if (elements.existsById(element.id().uuid())) {
      throw new ElementDeFabricationDejaExistantException(element.id());
    }
    elements.save(ElementDeFabricationEntity.from(element));

    return element;
  }

  @Override
  public ElementDeFabrication update(ElementDeFabrication element) {
    if (!elements.existsById(element.id().uuid())) {
      throw new ElementDeFabricationIntrouvableException(element.id());
    }
    elements.save(ElementDeFabricationEntity.from(element));

    return element;
  }

  @Override
  public void delete(ElementDeFabricationId id) {
    if (!elements.existsById(id.uuid())) {
      throw new ElementDeFabricationIntrouvableException(id);
    }
    elements.deleteById(id.uuid());
  }

  @Override
  public Optional<ElementDeFabrication> get(ElementDeFabricationId id) {
    return elements.findById(id.uuid()).map(ElementDeFabricationEntity::toDomain);
  }

  @Override
  public Page<ElementDeFabrication> list(ElementDeFabricationCriteria criteria, Pageable pageable) {
    Periode periode = criteria.periode();
    var page = elements.findByDateDeCreationBetween(
      periode.debut(),
      periode.fin(),
      PageRequest.of(pageable.page(), pageable.size(), PAR_DATE_DE_CREATION_DESCENDANTE)
    );

    return Page.<ElementDeFabrication>builder()
      .content(page.getContent().stream().map(ElementDeFabricationEntity::toDomain).toList())
      .currentPage(pageable.page())
      .pageSize(pageable.size())
      .totalElementsCount(page.getTotalElements());
  }
}
