package com.glm.glmback.syntheseheures.domain;

import java.time.Instant;
import java.util.Optional;

/**
 * Le dernier pointage d'OF actif d'un operateur sur une periode, tous elements confondus : la matiere de la fin
 * presumee d'une journee abandonnee. Il n'est demande que pour une telle journee.
 */
@FunctionalInterface
public interface PointagesDAtelier {
  Optional<Instant> dernierPointage(OperateurId operateur, Plage periode);
}
