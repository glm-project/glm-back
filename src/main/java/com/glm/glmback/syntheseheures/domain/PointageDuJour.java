package com.glm.glmback.syntheseheures.domain;

import java.time.Instant;

/**
 * Une ligne du journal brut d'un jour : un pointage de presence, ou un pointage sur un element.
 *
 * <p>
 * Deux sous-types parce qu'ils ne portent pas les memes champs : un pointage d'element nomme son element et son poste,
 * une arrivee ou un depart n'en a aucun.
 * </p>
 */
public sealed interface PointageDuJour permits EvenementDePresence, PointageDElement {
  Instant dateDeSurvenue();
}
