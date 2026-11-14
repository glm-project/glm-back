package com.glm.glmback.atelier.domain;

/**
 * L'audit des pointages ignores par la regle de reception, et des pointages dont l'horloge est trop en avance pour etre
 * juges.
 *
 * <p>
 * Une ligne par pointage, sans cle ni contrainte : deux lignes pour un meme renvoi sont acceptees. Elle s'ecrit
 * dans la transaction du jugement, qui n'est pas annulee par le refus renvoye au pupitre : le refus est leve apres le
 * commit.
 * </p>
 */
public interface PointagesIgnores {
  void enregistre(PointageIgnore pointage);

  void enregistre(PointageEnAvance pointage);

  /**
   * Vrai si un pointage de cet identifiant est deja a l'audit, ignore ou en avance : son renvoi est refuse comme un pointage
   * ignore, sans nouvelle ligne.
   */
  boolean contient(EvenementDAtelierId pointage);
}
