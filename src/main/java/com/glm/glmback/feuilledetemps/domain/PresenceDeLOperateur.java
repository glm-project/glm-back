package com.glm.glmback.feuilledetemps.domain;

import java.time.Instant;
import java.util.List;

/**
 * Les venues d'un operateur qui recouvrent une periode.
 *
 * <p>
 * Recouvrir, et non commencer dans : une equipe de nuit arrivee dimanche soir travaille encore le lundi matin, et sa
 * journee doit remonter meme si elle a commence avant la semaine demandee.
 * </p>
 */
public interface PresenceDeLOperateur {
  List<JourneeDeTravail> journeesRecouvrant(OperateurId operateur, Instant debut, Instant finExclusive);
}
