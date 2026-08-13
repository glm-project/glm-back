package com.glm.glmback.postedetravail.domain;

/**
 * Ce que ce contexte sait des pointages, sans jamais dependre du contexte de l'atelier.
 *
 * <p>
 * Depuis que le journal d'atelier ne retient que l'identifiant du poste, le supprimer laisserait des heures de travail
 * sans machine. La regle vit donc dans le domaine, et non dans une contrainte du schema.
 * </p>
 */
public interface PostesPointes {
  boolean aServiAPointer(PosteDeTravailId poste);
}
