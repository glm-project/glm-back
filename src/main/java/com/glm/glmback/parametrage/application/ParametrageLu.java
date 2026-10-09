package com.glm.glmback.parametrage.application;

import com.glm.glmback.parametrage.domain.Parametrage;
import com.glm.glmback.parametrage.domain.VersionDuLogo;
import com.glm.glmback.shared.error.domain.Assert;
import java.util.Optional;

/**
 * Ce que l'entreprise lit de son parametrage en un appel : ses reglages, et la version de son logo s'il en a un, sans
 * son contenu.
 */
public record ParametrageLu(Parametrage parametrage, Optional<VersionDuLogo> versionDuLogo) {
  public ParametrageLu {
    Assert.notNull("parametrage", parametrage);
    Assert.notNull("versionDuLogo", versionDuLogo);
  }
}
