package com.glm.glmback.pupitre.domain;

import java.util.List;

/**
 * Les operateurs de l'entreprise courante, avec les postes sur lesquels ils sont habilites.
 *
 * <p>
 * Le releve des presences entre par l'appel plutot que par une seconde traversee de la liste : l'operateur nait
 * complet, avec l'etat que le referentiel doit rendre, et l'adapter n'a qu'a interroger le releve — il ne decide rien.
 * </p>
 *
 * <p>
 * Aucune pagination, et c'est le point : la pagination est exactement ce qui casse l'instantane que le pupitre vient
 * chercher. Le volume est borne par la taille de l'atelier.
 * </p>
 */
@FunctionalInterface
public interface OperateursDuPupitre {
  List<OperateurDuPupitre> tous(PresencesDesOperateurs presences);
}
