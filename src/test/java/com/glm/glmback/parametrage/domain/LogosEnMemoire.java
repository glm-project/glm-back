package com.glm.glmback.parametrage.domain;

import java.util.Optional;

public final class LogosEnMemoire implements LogoRepository {

  private Logo logo;

  @Override
  public Optional<Logo> get() {
    return Optional.ofNullable(logo);
  }

  @Override
  public Logo update(Logo nouveau) {
    logo = nouveau;

    return nouveau;
  }
}
