package com.glm.glmback.atelier.application.gestionconflits;

import com.glm.glmback.atelier.domain.gestionconflits.LectureDossierConflit;
import java.time.Instant;

public record ApercuDeResolution(
  PropositionAConfirmer proposition,
  Instant evaluation,
  LectureDossierConflit avant,
  LectureDossierConflit apres
) {}
