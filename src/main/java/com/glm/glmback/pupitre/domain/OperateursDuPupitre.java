package com.glm.glmback.pupitre.domain;

import java.util.List;

/**
 * Les operateurs de l'entreprise courante, avec les postes sur lesquels ils sont habilites.
 *
 * <p>
 * Aucune pagination, et c'est le point : la pagination est exactement ce qui casse l'instantane que le pupitre vient
 * chercher. Le volume est borne par la taille de l'atelier.
 * </p>
 */
@FunctionalInterface
public interface OperateursDuPupitre {
  List<OperateurDuPupitre> tous();
}
