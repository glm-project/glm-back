package com.glm.glmback.atelier.application.gestionanomalies;

import com.glm.glmback.atelier.domain.gestionanomalies.LectureDossierAnomalie;
import java.time.Instant;

public record ApercuDeResolution(
  PropositionAConfirmer proposition,
  Instant evaluation,
  LectureDossierAnomalie avant,
  LectureDossierAnomalie apres
) {}
