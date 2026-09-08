package com.glm.glmback.coutderevient.domain;

import java.util.Optional;

/**
 * Le referentiel des elements de fabrication, atteint par identifiant.
 *
 * <p>
 * Rien n'en est copie : le rapport parle de l'element, donc un element renomme doit s'afficher renomme, y compris
 * sur ses heures anciennes.
 * </p>
 */
@FunctionalInterface
public interface ElementsValorisables {
  Optional<ElementValorise> get(ElementId element);
}
