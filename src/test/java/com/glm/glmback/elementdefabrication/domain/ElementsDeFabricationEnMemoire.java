package com.glm.glmback.elementdefabrication.domain;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Doublure de test du port de persistance : elle laisse les tests du domaine se passer d'une base.
 */
final class ElementsDeFabricationEnMemoire implements ElementDeFabricationRepository {

  private final Map<ElementDeFabricationId, ElementDeFabrication> elements = new ConcurrentHashMap<>();

  @Override
  public ElementDeFabrication create(ElementDeFabrication element) {
    ElementDeFabrication existant = elements.putIfAbsent(element.id(), element);
    if (existant != null) {
      throw new ElementDeFabricationDejaExistantException(element.id());
    }

    return element;
  }

  @Override
  public ElementDeFabrication update(ElementDeFabrication element) {
    ElementDeFabrication precedent = elements.replace(element.id(), element);
    if (precedent == null) {
      throw new ElementDeFabricationIntrouvableException(element.id());
    }

    return element;
  }

  @Override
  public void delete(ElementDeFabricationId id) {
    ElementDeFabrication supprime = elements.remove(id);
    if (supprime == null) {
      throw new ElementDeFabricationIntrouvableException(id);
    }
  }

  @Override
  public Optional<ElementDeFabrication> get(ElementDeFabricationId id) {
    return Optional.ofNullable(elements.get(id));
  }

  @Override
  public Optional<ElementDeFabricationId> idPourReference(Reference reference) {
    return elements
      .values()
      .stream()
      .filter(element -> element.reference().filter(reference::equals).isPresent())
      .findFirst()
      .map(ElementDeFabrication::id);
  }
}
