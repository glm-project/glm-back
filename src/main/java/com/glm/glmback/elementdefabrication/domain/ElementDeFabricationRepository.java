package com.glm.glmback.elementdefabrication.domain;

import java.util.Optional;

public interface ElementDeFabricationRepository {
  ElementDeFabrication create(ElementDeFabrication element);

  ElementDeFabrication update(ElementDeFabrication element);

  void delete(ElementDeFabricationId id);

  Optional<ElementDeFabrication> get(ElementDeFabricationId id);

  Optional<ElementDeFabricationId> idPourReference(Reference reference);
}
