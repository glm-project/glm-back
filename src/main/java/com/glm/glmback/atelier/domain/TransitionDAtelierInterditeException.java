package com.glm.glmback.atelier.domain;

/**
 * Un geste qui vise une activite que le journal ne lui permet pas de terminer ou de remplacer a son heure : deja
 * terminee, deja remplacee, annulee, ouverte apres lui, ou de la meme categorie que la transition qui la vise.
 */
public final class TransitionDAtelierInterditeException extends RuntimeException {

  public TransitionDAtelierInterditeException(EvenementDAtelier evenement) {
    super(
      "Le pointage %s de %s du %s contredit le journal : l'activite visee %s n'est pas en cours a son heure%s".formatted(
        evenement.type(),
        evenement.operateur().uuid(),
        evenement.dateDeSurvenue(),
        evenement.activiteVisee().map(ActiviteId::uuid).orElseThrow(),
        evenement.intention() == IntentionDePointage.TRANSITION ? " dans l'autre categorie" : ""
      )
    );
  }
}
