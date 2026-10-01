package com.glm.glmback.atelier.domain;

/**
 * Ce qu'un pointage fait d'une activite, que son type seul ne dit pas.
 *
 * <p>
 * Une {@link #OUVERTURE} cree une activite nouvelle, en travail ou en non conformite, y compris a la reprise apres une
 * pause ; si une activite est deja en cours sur le meme poste, elle la termine a son heure (relance). Une
 * {@link #TRANSITION} remplace l'activite qu'elle vise par une activite de l'autre categorie. Une {@link #FIN} termine
 * l'activite qu'elle vise.
 * </p>
 *
 * <p>
 * Une transition dont la cible n'est plus en cours reste une transition : elle ne devient jamais implicitement une
 * ouverture. Le debut et la non conformite se pointent donc aussi bien en ouverture qu'en transition, et seule une fin
 * se pointe FIN.
 * </p>
 */
public enum IntentionDePointage {
  OUVERTURE,
  TRANSITION,
  FIN;

  /**
   * Vrai pour une ouverture et une transition : le pointage ouvre une activite, qui prend son identite.
   */
  public boolean ouvreUneActivite() {
    return this != FIN;
  }

  /**
   * Vrai pour une transition et une fin : le pointage vise l'activite qu'il remplace ou termine.
   */
  public boolean viseUneActivite() {
    return this != OUVERTURE;
  }

  public boolean admet(TypeDEvenementDAtelier type) {
    return (this == FIN) == (type == TypeDEvenementDAtelier.FIN);
  }

  /**
   * L'ordre de deux gestes survenus a la meme heure sur un meme poste : la fin, puis la transition, puis l'ouverture.
   * Jamais leur date d'enregistrement, qui ferait dependre le journal de l'ordre de reception.
   */
  int rangAHeureEgale() {
    return switch (this) {
      case FIN -> 0;
      case TRANSITION -> 1;
      case OUVERTURE -> 2;
    };
  }
}
