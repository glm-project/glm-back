package com.glm.glmback.syntheseheures.domain;

import java.time.Instant;
import java.util.List;

/** Lecture du journal brut, independante de la selection du travail. */
public interface JournalDeLOperateur {
  List<JournalDElement> dans(OperateurId operateur, Instant debut, Instant finExclusive);
}
