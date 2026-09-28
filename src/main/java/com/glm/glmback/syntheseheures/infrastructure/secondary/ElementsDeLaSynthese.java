package com.glm.glmback.syntheseheures.infrastructure.secondary;

import com.glm.glmback.syntheseheures.domain.ElementId;
import com.glm.glmback.syntheseheures.domain.ElementsDeFabrication;
import com.glm.glmback.syntheseheures.domain.FicheDElement;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

/**
 * Les fiches des elements, lues dans la table du referentiel sans importer son code, en une seule requete.
 */
@Repository
class ElementsDeLaSynthese implements ElementsDeFabrication {

  private final SpringDataElementsDeLaSyntheseRepository elements;

  ElementsDeLaSynthese(SpringDataElementsDeLaSyntheseRepository elements) {
    this.elements = elements;
  }

  @Override
  public List<FicheDElement> parIds(Set<ElementId> ids) {
    return elements
      .findByIdIn(ids.stream().map(ElementId::uuid).collect(Collectors.toSet()))
      .stream()
      .map(ElementDeLaSyntheseEntity::toDomain)
      .toList();
  }
}
