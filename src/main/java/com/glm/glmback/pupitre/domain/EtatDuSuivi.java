package com.glm.glmback.pupitre.domain;

/**
 * L'etat d'un element pointable, tel que la tuile du pupitre l'affiche.
 *
 * <p>
 * Pas de {@code CLOTURE} ici, contrairement a l'atelier : un element cloture n'accepte plus de pointage, il ne figure
 * donc pas au referentiel du pupitre. La valeur n'aurait aucun porteur.
 * </p>
 */
public enum EtatDuSuivi {
  EN_ATTENTE,
  EN_COURS,
  INTERROMPU,
}
