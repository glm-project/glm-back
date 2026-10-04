package com.glm.glmback.atelier.domain;

public enum CodeDeProposition {
  RATTACHER_FIN_A_ACTIVITE_REMPLACANTE,
  ANNULER_TRANSITION;

  public TypeDActeDeResolution kind() {
    return switch (this) {
      case RATTACHER_FIN_A_ACTIVITE_REMPLACANTE -> TypeDActeDeResolution.CORRECTION;
      case ANNULER_TRANSITION -> TypeDActeDeResolution.ANNULATION;
    };
  }
}
