package com.glm.glmback.atelier.domain;

import java.time.Instant;

/**
 * Ce que le gestionnaire a fait d'un pointage en attente : l'appliquer par une regularisation, ou l'ecarter motif a
 * l'appui. Seul l'ecart porte un motif, la regularisation produite portant deja son auteur.
 */
public sealed interface TraitementDuPointage permits Application, Ecart {
  Auteur auteur();

  Instant date();
}
