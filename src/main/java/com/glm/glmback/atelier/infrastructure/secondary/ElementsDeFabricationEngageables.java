package com.glm.glmback.atelier.infrastructure.secondary;

import com.glm.glmback.atelier.domain.ElementEngage;
import com.glm.glmback.atelier.domain.ElementEngageId;
import com.glm.glmback.atelier.domain.ElementsEngageables;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * Ce que l'atelier peut engager : les elements de fabrication declares par l'entreprise courante.
 */
@Repository
class ElementsDeFabricationEngageables implements ElementsEngageables {

  private final SpringDataElementsEngageablesRepository elements;

  ElementsDeFabricationEngageables(SpringDataElementsEngageablesRepository elements) {
    this.elements = elements;
  }

  @Override
  public Optional<ElementEngage> get(ElementEngageId id) {
    return elements.findById(id.uuid()).map(ElementEngageableEntity::toDomain);
  }
}
