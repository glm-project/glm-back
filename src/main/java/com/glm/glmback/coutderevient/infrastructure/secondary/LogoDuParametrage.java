package com.glm.glmback.coutderevient.infrastructure.secondary;

import com.glm.glmback.coutderevient.domain.LogoDeLEntreprise;
import com.glm.glmback.coutderevient.domain.LogosDeLEntreprise;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** Le logo, lu dans la ligne unique du parametrage que le schema de chaque entreprise porte des sa creation. */
@Repository
class LogoDuParametrage implements LogosDeLEntreprise {

  private static final int LIGNE_UNIQUE = 1;

  private final SpringDataLogosDuCoutRepository logos;

  LogoDuParametrage(SpringDataLogosDuCoutRepository logos) {
    this.logos = logos;
  }

  @Override
  public Optional<LogoDeLEntreprise> logo() {
    return logos.findById(LIGNE_UNIQUE).orElseThrow().toDomain();
  }
}
