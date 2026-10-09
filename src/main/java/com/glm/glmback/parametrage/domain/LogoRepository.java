package com.glm.glmback.parametrage.domain;

import java.util.Optional;

public interface LogoRepository {
  Optional<Logo> get();

  Optional<VersionDuLogo> version();

  Logo update(Logo logo);
}
