package com.glm.glmback.atelier.domain.gestionanomalies;

public enum CodeDeProposition {
  RATTACHER_FIN_A_ACTIVITE_REMPLACANTE,
  ANNULER_TRANSITION,
  REGULARISER_FIN,
  CORRIGER_FIN_TARDIVE,
  CORRIGER_TRANSITION_TARDIVE;

  public TypeDActeDeResolution kind() {
    return switch (this) {
      case RATTACHER_FIN_A_ACTIVITE_REMPLACANTE, CORRIGER_FIN_TARDIVE, CORRIGER_TRANSITION_TARDIVE -> TypeDActeDeResolution.CORRECTION;
      case ANNULER_TRANSITION -> TypeDActeDeResolution.ANNULATION;
      case REGULARISER_FIN -> TypeDActeDeResolution.REGULARISATION;
    };
  }
}
