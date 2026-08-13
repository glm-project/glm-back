package com.glm.glmback.operateur.domain;

/**
 * Ce que ce contexte sait des pointages, sans jamais dependre du contexte de l'atelier.
 *
 * <p>
 * Le journal d'atelier et les journees de travail ne retiennent que l'identifiant de l'operateur : le supprimer
 * laisserait des heures de presence sans personne a payer.
 * </p>
 */
public interface OperateursQuiOntPointe {
  boolean aPointe(OperateurId operateur);
}
