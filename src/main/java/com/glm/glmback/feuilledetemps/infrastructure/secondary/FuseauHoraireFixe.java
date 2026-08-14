package com.glm.glmback.feuilledetemps.infrastructure.secondary;

import com.glm.glmback.feuilledetemps.domain.FuseauHoraireDeLEntreprise;
import java.time.ZoneId;
import org.springframework.stereotype.Component;

/**
 * Le fuseau des entreprises servies aujourd'hui, fixe pour l'instant.
 *
 * <p>
 * Meme parti que les prefixes d'elements de fabrication : la donnee de parametrage passe deja par un port, donc la
 * remplacer par une valeur declaree par entreprise ne touchera pas une ligne du domaine.
 * </p>
 */
@Component
public class FuseauHoraireFixe implements FuseauHoraireDeLEntreprise {

  private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

  @Override
  public ZoneId zone() {
    return PARIS;
  }
}
