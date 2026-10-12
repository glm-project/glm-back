package com.glm.glmback.elementdefabrication.domain;

/**
 * Ou en est un element de fabrication, lu sur son suivi d'atelier : un element n'est que dans un cycle a la fois.
 */
public enum CycleDeVie {
  /** Aucun suivi : l'element est cree, pas encore mis a l'atelier. */
  EN_PREPARATION,
  /** Son suivi est ouvert. */
  EN_ATELIER,
  /** Son suivi est cloture. */
  CLOTURE,
}
