package com.glm.glmback.atelier.domain;

/**
 * Ce qu'est devenu un pointage a son arrivee.
 */
public enum IssueDePointage {
  /** Il est entre au journal. */
  ACCEPTE,
  /** Son identifiant figurait deja dans la table des evenements : rien n'est ecrit, la reponse est un succes. */
  REJOUE,
  /** Il est parti en audit, sans entrer au journal ; le pupitre en recoit le refus. */
  IGNORE,
}
