package com.glm.glmback.coutderevient.infrastructure.secondary;

import com.glm.glmback.coutderevient.domain.FuseauHoraireDeLEntreprise;
import java.time.ZoneId;
import org.springframework.stereotype.Component;

/**
 * Le fuseau des entreprises servies aujourd'hui, fixe pour l'instant : le meme que celui des feuilles de temps.
 *
 * <p>
 * La donnee passe deja par un port : la remplacer par une valeur declaree par entreprise ne touchera pas le domaine.
 * </p>
 */
@Component
class FuseauHoraireDuCout implements FuseauHoraireDeLEntreprise {

  private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

  @Override
  public ZoneId zone() {
    return PARIS;
  }
}
