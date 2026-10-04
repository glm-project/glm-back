package com.glm.glmback.atelier.application.gestionconflits;

import com.glm.glmback.atelier.domain.gestionconflits.LectureDossierConflit;

public record ApercuDeResolution(ReferenceDApercu reference, LectureDossierConflit avant, LectureDossierConflit apres) {}
