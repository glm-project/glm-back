package com.glm.glmback.operateur.domain;

/**
 * Ce que ce contexte sait des pointages, sans jamais dependre du contexte de l'atelier.
 *
 * <p>
 * Tout fait historique du journal d'atelier, meme annule, conserve l'identifiant de l'operateur : sa fiche reste
 * necessaire pour lire l'histoire des activites.
 * </p>
 */
public interface OperateursQuiOntPointe {
  boolean aPointe(OperateurId operateur);
}
