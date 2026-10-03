package com.glm.glmback.atelier.application;

import com.glm.glmback.atelier.domain.LectureDossierConflit;

public record ApercuDeResolution(ReferenceDApercu reference, LectureDossierConflit avant, LectureDossierConflit apres) {}
