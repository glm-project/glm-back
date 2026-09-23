package com.glm.glmback.pupitre.domain;

/**
 * L'etat de presence des operateurs de l'entreprise courante, releve en une fois.
 *
 * <p>
 * Ni critere ni pagination, pour la meme raison que {@link OperateursDuPupitre} : c'est un releve d'un seul tenant,
 * dont le cout ne croit pas avec le nombre d'operateurs.
 * </p>
 */
@FunctionalInterface
public interface PresencesDuPupitre {
  PresencesDesOperateurs toutes();
}
