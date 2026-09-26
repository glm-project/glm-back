package com.glm.glmback.atelier.domain;

import java.time.Instant;
import java.util.Optional;

/**
 * Le geste du pupitre, conserve tel quel. La presence et l'atelier ne portent pas les memes champs : un geste
 * d'atelier vise un suivi et, facultativement, un poste.
 */
public sealed interface GesteEnAttente permits GesteDePresence, GesteDAtelier {
  OperateurId operateur();

  Optional<Instant> dateDeclaree();
}
