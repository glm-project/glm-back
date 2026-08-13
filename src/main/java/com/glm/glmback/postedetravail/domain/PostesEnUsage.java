package com.glm.glmback.postedetravail.domain;

/**
 * Ce que ce contexte sait des habilitations, sans jamais dependre du contexte des operateurs.
 *
 * <p>
 * La regle qui interdit de supprimer un poste encore habilite vit ainsi dans le domaine, et non dans une contrainte du
 * schema : celle-ci n'est que le filet de dernier recours.
 * </p>
 */
public interface PostesEnUsage {
  boolean estHabilite(PosteDeTravailId poste);
}
