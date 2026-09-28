package com.glm.glmback.syntheseheures.infrastructure.primary;

import com.glm.glmback.syntheseheures.domain.TypeDEvenementDAtelier;
import com.glm.glmback.syntheseheures.domain.TypeDEvenementDePresence;

/**
 * Les cinq pointages que le journal du jour peut porter : deux de presence, trois sur un element.
 */
enum RestTypeDePointage {
  ARRIVEE,
  DEPART,
  DEBUT,
  NON_CONFORMITE,
  FIN;

  static RestTypeDePointage from(TypeDEvenementDePresence type) {
    return switch (type) {
      case ARRIVEE -> ARRIVEE;
      case DEPART -> DEPART;
    };
  }

  static RestTypeDePointage from(TypeDEvenementDAtelier type) {
    return switch (type) {
      case DEBUT -> DEBUT;
      case NON_CONFORMITE -> NON_CONFORMITE;
      case FIN -> FIN;
    };
  }
}
