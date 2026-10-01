package com.glm.glmback.atelier.infrastructure.secondary;

import com.glm.glmback.atelier.domain.ElementEngage;
import com.glm.glmback.atelier.domain.ElementEngageId;
import com.glm.glmback.atelier.domain.ElementsEngageables;
import com.glm.glmback.atelier.domain.ReferencesDElements;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

/**
 * Ce que l'atelier peut engager : les elements de fabrication declares par l'entreprise courante.
 */
@Repository
class ElementsDeFabricationEngageables implements ElementsEngageables, ReferencesDElements {

  private final SpringDataElementsEngageablesRepository elements;

  ElementsDeFabricationEngageables(SpringDataElementsEngageablesRepository elements) {
    this.elements = elements;
  }

  @Override
  public Optional<ElementEngage> get(ElementEngageId id) {
    return elements.findById(id.uuid()).map(ElementEngageableEntity::toDomain);
  }

  @Override
  public Map<ElementEngageId, String> parIds(Set<ElementEngageId> identifiants) {
    return elements
      .findAllById(identifiants.stream().map(ElementEngageId::uuid).toList())
      .stream()
      .filter(element -> element.reference() != null)
      .collect(Collectors.toUnmodifiableMap(ElementEngageableEntity::id, ElementEngageableEntity::reference));
  }
}
