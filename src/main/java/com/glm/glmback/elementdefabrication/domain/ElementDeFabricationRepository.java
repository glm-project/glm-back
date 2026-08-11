package com.glm.glmback.elementdefabrication.domain;

import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import java.util.Optional;

public interface ElementDeFabricationRepository {
  ElementDeFabrication create(ElementDeFabrication element);

  ElementDeFabrication update(ElementDeFabrication element);

  void delete(ElementDeFabricationId id);

  Optional<ElementDeFabrication> get(ElementDeFabricationId id);

  Optional<ElementDeFabricationId> idPourReference(Reference reference);

  Page<ElementDeFabrication> list(ElementDeFabricationCriteria criteria, Pageable pageable);
}
