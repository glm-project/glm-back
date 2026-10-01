package com.glm.glmback.syntheseheures.domain;

import java.util.List;

/** Les conflits projetes par atelier pour l'operateur, sans reconstruire son journal. */
public interface ConflitsDeLOperateur {
  List<SequenceEnConflit> de(OperateurId operateur);
}
