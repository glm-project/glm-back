package com.glm.glmback.atelier.domain;

/**
 * Un geste qui vise l'activite d'un autre operateur, ou d'un autre poste de travail : une fin ou une transition ne
 * touche que l'activite de son propre couple operateur/poste. Le refus est definitif.
 */
public final class ActiviteViseeIncoherenteException extends RuntimeException {

  public ActiviteViseeIncoherenteException(EvenementDAtelier geste, ActiviteId visee) {
    super(
      "L'activite %s visee par le pointage %s de %s du %s appartient a un autre operateur ou a un autre poste".formatted(
        visee.uuid(),
        geste.type(),
        geste.operateur().uuid(),
        geste.dateDeSurvenue()
      )
    );
  }
}
