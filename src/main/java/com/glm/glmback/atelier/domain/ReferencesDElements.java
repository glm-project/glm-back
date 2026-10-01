package com.glm.glmback.atelier.domain;

import java.util.Map;
import java.util.Set;

/** Les references actuelles des elements, resolues ensemble pour une page de suivis. */
public interface ReferencesDElements {
  Map<ElementEngageId, String> parIds(Set<ElementEngageId> elements);
}
