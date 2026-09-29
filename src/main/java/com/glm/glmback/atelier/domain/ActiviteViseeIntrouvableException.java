package com.glm.glmback.atelier.domain;

/**
 * Un geste qui vise une activite qu'aucun pointage de ce suivi n'a jamais ouverte. Le refus est definitif : rejouer le
 * meme geste ne la fera pas apparaitre.
 */
public final class ActiviteViseeIntrouvableException extends RuntimeException {

  public ActiviteViseeIntrouvableException(EvenementDAtelier geste, ActiviteId visee) {
    super(
      "L'activite %s visee par le pointage %s de %s du %s est introuvable dans ce suivi".formatted(
        visee.uuid(),
        geste.type(),
        geste.operateur().uuid(),
        geste.dateDeSurvenue()
      )
    );
  }
}
