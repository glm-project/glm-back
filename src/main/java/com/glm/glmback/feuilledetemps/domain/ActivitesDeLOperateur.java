package com.glm.glmback.feuilledetemps.domain;

import java.time.Instant;
import java.util.List;

/** Activites interpretees par atelier qui recouvrent la periode, sans borne basse sur leur debut. */
@FunctionalInterface
public interface ActivitesDeLOperateur {
  List<ActiviteInterpretee> recouvrant(OperateurId operateur, Instant debut, Instant finExclusive);
}
