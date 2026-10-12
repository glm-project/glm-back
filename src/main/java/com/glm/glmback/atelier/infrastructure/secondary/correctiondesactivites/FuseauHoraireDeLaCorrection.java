package com.glm.glmback.atelier.infrastructure.secondary.correctiondesactivites;

import com.glm.glmback.atelier.domain.correctiondesactivites.FuseauHoraireDeLEntreprise;
import java.time.ZoneId;
import org.springframework.stereotype.Component;

/**
 * Le fuseau des entreprises servies aujourd'hui, fixe pour l'instant.
 *
 * <p>
 * Meme parti que {@code feuilledetemps.FuseauHoraireFixe} : la donnee de parametrage passe deja par un port, donc la
 * remplacer par une valeur declaree par entreprise ne touchera pas une ligne du domaine. Nommee differemment pour ne
 * pas entrer en conflit de bean Spring avec ses jumeaux.
 * </p>
 */
@Component
class FuseauHoraireDeLaCorrection implements FuseauHoraireDeLEntreprise {

  private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

  @Override
  public ZoneId zone() {
    return PARIS;
  }
}
