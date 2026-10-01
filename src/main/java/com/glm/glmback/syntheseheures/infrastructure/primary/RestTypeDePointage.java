package com.glm.glmback.syntheseheures.infrastructure.primary;

import com.glm.glmback.syntheseheures.domain.TypeDEvenementDAtelier;

/**
 * Les trois gestes du journal d'element.
 */
enum RestTypeDePointage {
  DEBUT,
  NON_CONFORMITE,
  FIN;

  static RestTypeDePointage from(TypeDEvenementDAtelier type) {
    return switch (type) {
      case DEBUT -> DEBUT;
      case NON_CONFORMITE -> NON_CONFORMITE;
      case FIN -> FIN;
    };
  }
}
