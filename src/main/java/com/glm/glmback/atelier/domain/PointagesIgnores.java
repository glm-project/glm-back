package com.glm.glmback.atelier.domain;

/**
 * L'audit des pointages ignores par la regle de reception.
 *
 * <p>
 * Une ligne par pointage ignore, sans cle ni contrainte : deux lignes pour un meme renvoi sont acceptees. Elle s'ecrit
 * dans la transaction du jugement, qui n'est pas annulee par le refus renvoye au pupitre : le refus est leve apres le
 * commit.
 * </p>
 */
public interface PointagesIgnores {
  void enregistre(PointageIgnore pointage);

  /**
   * Vrai si un pointage de cet identifiant a deja ete ignore : son renvoi est refuse de la meme facon, sans nouvelle ligne.
   */
  boolean contient(EvenementDAtelierId pointage);
}
