package com.glm.glmback.coutderevient.domain;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Le referentiel des elements de fabrication, atteint par identifiant.
 *
 * <p>
 * Rien n'en est copie : le rapport parle de l'element, donc un element renomme doit s'afficher renomme, y compris
 * sur ses heures anciennes.
 * </p>
 */
public interface ElementsValorisables {
  Optional<ElementValorise> get(ElementId element);

  List<ElementValorise> tous(Set<ElementId> elements);
}
