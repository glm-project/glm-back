package com.glm.glmback.atelier.domain.correctiondesactivites;

import com.glm.glmback.atelier.domain.ActiviteId;
import com.glm.glmback.atelier.domain.PosteDeTravailId;

/**
 * Un changement de poste vers le poste que l'activite occupe deja : il ne changerait rien. La page ne le propose pas ;
 * ce refus ne survient que si l'activite a change de poste depuis sa lecture.
 */
public final class MemePosteException extends RuntimeException {

  public MemePosteException(ActiviteId activite, PosteDeTravailId poste) {
    super("L'activite %s est deja sur le poste %s".formatted(activite.uuid(), poste.uuid()));
  }
}
