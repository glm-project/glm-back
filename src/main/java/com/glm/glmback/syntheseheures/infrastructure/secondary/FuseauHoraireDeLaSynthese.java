package com.glm.glmback.syntheseheures.infrastructure.secondary;

import com.glm.glmback.syntheseheures.domain.FuseauHoraireDeLEntreprise;
import java.time.ZoneId;
import org.springframework.stereotype.Component;

/**
 * Le fuseau des entreprises servies aujourd'hui, fixe pour l'instant.
 *
 * <p>
 * Meme parti que {@code feuilledetemps.FuseauHoraireFixe} : la donnee de parametrage passe deja par un port, donc la
 * remplacer par une valeur declaree par entreprise ne touchera pas une ligne du domaine. Nommee differemment pour ne
 * pas entrer en conflit de bean Spring avec son jumeau.
 * </p>
 */
@Component
public class FuseauHoraireDeLaSynthese implements FuseauHoraireDeLEntreprise {

  private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

  @Override
  public ZoneId zone() {
    return PARIS;
  }
}
