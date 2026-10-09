package com.glm.glmback.parametrage.domain;

import java.util.Optional;

public interface LogoRepository {
  Optional<Logo> get();

  Logo update(Logo logo);
}
