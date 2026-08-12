package com.glm.glmback.atelier.domain;

public final class EvenementAvantEngagementException extends RuntimeException {

  public EvenementAvantEngagementException(EvenementDAtelier evenement) {
    super(
      "L'evenement %s du %s est anterieur a l'engagement de l'element dans l'atelier".formatted(
        evenement.type(),
        evenement.dateDeSurvenue()
      )
    );
  }
}
