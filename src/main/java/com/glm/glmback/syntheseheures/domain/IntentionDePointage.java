package com.glm.glmback.syntheseheures.domain;

/** Intention du fait, relue sans la deduire de son type. */
public enum IntentionDePointage {
  OUVERTURE,
  TRANSITION,
  FIN;

  int rangAHeureEgale() {
    return switch (this) {
      case FIN -> 0;
      case TRANSITION -> 1;
      case OUVERTURE -> 2;
    };
  }
}
