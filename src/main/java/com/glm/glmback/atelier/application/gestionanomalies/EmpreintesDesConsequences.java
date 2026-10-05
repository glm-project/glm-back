package com.glm.glmback.atelier.application.gestionanomalies;

import com.glm.glmback.atelier.domain.SuiviDAtelier;
import java.time.Instant;

public interface EmpreintesDesConsequences {
  String calcule(SuiviDAtelier apres, Instant evaluation);
}
