package com.glm.glmback.atelier.gestionconflits.application;

import com.glm.glmback.atelier.gestionconflits.domain.LectureDossierConflit;

public record ApercuDeResolution(ReferenceDApercu reference, LectureDossierConflit avant, LectureDossierConflit apres) {}
