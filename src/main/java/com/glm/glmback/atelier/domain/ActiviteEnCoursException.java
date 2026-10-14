package com.glm.glmback.atelier.domain;

/**
 * Une correction qui vise une activite que rien n'a terminee et dont l'echeance n'est pas atteinte : le pupitre la tient
 * ouverte. Retirer une activite ou changer son poste, c'est corriger un fait passe ; une activite en cours ne l'est pas.
 */
public final class ActiviteEnCoursException extends RuntimeException {

  public ActiviteEnCoursException(ActiviteId activite) {
    super("L'activite %s est en cours : elle ne se corrige qu'une fois terminee".formatted(activite.uuid()));
  }
}
