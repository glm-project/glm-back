package com.glm.glmback.coutderevient.infrastructure.secondary;

import com.glm.glmback.coutderevient.domain.ElementId;
import com.glm.glmback.coutderevient.domain.ElementValorise;
import com.glm.glmback.coutderevient.domain.ElementsValorisables;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class ElementsDeFabricationValorisables implements ElementsValorisables {

  private final SpringDataElementsValorisablesRepository elements;

  ElementsDeFabricationValorisables(SpringDataElementsValorisablesRepository elements) {
    this.elements = elements;
  }

  @Override
  public Optional<ElementValorise> get(ElementId element) {
    return elements.findById(element.uuid()).map(ElementValorisableEntity::toDomain);
  }
}
