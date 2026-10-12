package com.glm.glmback.syntheseheures.infrastructure.secondary;

import com.glm.glmback.syntheseheures.domain.LogoDeLEntreprise;
import com.glm.glmback.syntheseheures.domain.LogosDeLEntreprise;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** Le logo, lu dans la ligne unique du parametrage que le schema de chaque entreprise porte des sa creation. */
@Repository("logoDuParametrageDuRapport")
class LogoDuParametrageDuRapport implements LogosDeLEntreprise {

  private static final int LIGNE_UNIQUE = 1;

  private final SpringDataLogosDuRapportRepository logos;

  LogoDuParametrageDuRapport(SpringDataLogosDuRapportRepository logos) {
    this.logos = logos;
  }

  @Override
  public Optional<LogoDeLEntreprise> logo() {
    return logos.findById(LIGNE_UNIQUE).orElseThrow().toDomain();
  }
}
