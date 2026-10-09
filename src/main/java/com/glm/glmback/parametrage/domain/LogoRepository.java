package com.glm.glmback.parametrage.domain;

import java.util.Optional;

public interface LogoRepository {
  Optional<Logo> get();

  Optional<VersionDuLogo> version();

  Logo update(Logo logo);

  /**
   * Sans effet si l'entreprise n'a pas de logo : la ligne du parametrage existe toujours.
   */
  void delete();
}
