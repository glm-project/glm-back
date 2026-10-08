package com.glm.glmback.atelier.domain;

/**
 * Le suivi apres un pointage, et si le pointage n'a rien ecrit : un renvoi dont l'identifiant figure deja dans la table
 * des evenements, ou une fin posterieure a la cloture, que la cloture a deja terminee.
 */
public record PointageDAtelierTraite(SuiviDAtelier suivi, boolean sansEcriture) {}
