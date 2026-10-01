package com.glm.glmback.syntheseheures.domain;

import java.time.Instant;
import java.util.List;

/** Activites qui recouvrent la periode, independamment de leurs pointages. */
public interface ActivitesDeLOperateur {
  List<ActiviteDElement> recouvrant(OperateurId operateur, Instant debut, Instant finExclusive);
}
