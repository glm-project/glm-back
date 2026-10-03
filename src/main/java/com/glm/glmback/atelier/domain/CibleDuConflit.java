package com.glm.glmback.atelier.domain;

import java.util.Optional;

public record CibleDuConflit(ActiviteId activite, Optional<EvenementDAtelierId> ouvrant, Optional<EvenementDAtelierId> termineePar) {}
