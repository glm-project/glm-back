package com.glm.glmback.atelier.domain;

import java.util.Optional;

/**
 * Ce que l'atelier sait des elements de fabrication, sans jamais dependre de leur contexte.
 */
public interface ElementsEngageables {
  Optional<ElementEngage> get(ElementEngageId id);
}
