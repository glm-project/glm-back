package com.glm.glmback.atelier.domain;

import java.util.List;

/**
 * Ce que l'atelier sait des habilitations, sans jamais dependre du contexte des operateurs.
 *
 * <p>
 * La regle est deliberement une regle dure : un pointage sur un poste non habilite est refuse. Elle ne joue que
 * lorsqu'un poste est fourni, l'invariant du poste facultatif restant entier pour une entreprise sans parc machine.
 * </p>
 */
public interface Habilitations {
  boolean estHabilite(OperateurId operateur, PosteDeTravailId poste);

  /**
   * Les postes sur lesquels l'operateur est habilite aujourd'hui : les habilitations n'ont aucune date, elles se lisent
   * toujours telles qu'elles sont.
   */
  List<PosteDeTravailId> postesHabilites(OperateurId operateur);
}
