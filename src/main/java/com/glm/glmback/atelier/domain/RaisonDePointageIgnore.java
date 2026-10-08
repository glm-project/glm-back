package com.glm.glmback.atelier.domain;

/**
 * Pourquoi la regle de reception ignore un pointage : il part en audit, sans jamais entrer au journal.
 */
public enum RaisonDePointageIgnore {
  /**
   * Un debut ou une non conformite alors qu'une activite est en cours sur la cle : double appui, ou deux pupitres hors
   * ligne sur le meme operateur.
   */
  DEJA_EN_COURS,
  /**
   * Une fin alors que rien n'est en cours sur la cle, et que la derniere activite n'est pas une fin automatique : un
   * second arret, ou une fin apres la cloture.
   */
  AUCUNE_ACTIVITE,
  /**
   * Une fin alors que la derniere activite de la cle a atteint son echeance sans fin reelle : l'echeance se juge sur
   * l'heure du geste, a l'echeance pile comprise.
   */
  APRES_ECHEANCE,
  /**
   * Un pointage dont l'heure de geste precede celle du dernier pointage accepte de la cle, regularisations comprises :
   * premier arrive, premier servi.
   */
  ANTERIEUR,
}
