package com.glm.glmback.pupitre.infrastructure.secondary;

import com.glm.glmback.pupitre.domain.LogoDuPupitre;
import com.glm.glmback.pupitre.domain.VersionDuLogo;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * La version du logo, lue dans la ligne unique du parametrage que le schema de chaque entreprise porte des sa creation.
 */
@Repository
class LogoDuReferentielDuPupitre implements LogoDuPupitre {

  private static final int LIGNE_UNIQUE = 1;

  private final SpringDataLogoDuPupitreRepository logos;

  LogoDuReferentielDuPupitre(SpringDataLogoDuPupitreRepository logos) {
    this.logos = logos;
  }

  @Override
  public Optional<VersionDuLogo> version() {
    return logos.findById(LIGNE_UNIQUE).orElseThrow().toDomain();
  }
}
