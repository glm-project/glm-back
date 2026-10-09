package com.glm.glmback.parametrage.infrastructure.secondary;

import com.glm.glmback.parametrage.domain.ParametrageRepository;
import com.glm.glmback.shared.activityduration.domain.MaximumActivityDuration;
import com.glm.glmback.shared.activityduration.domain.MaximumActivityDurations;
import org.springframework.stereotype.Component;

/**
 * La duree max d'une activite que le gestionnaire a fixee, rendue aux contextes qui l'appliquent par le port du noyau
 * partage.
 *
 * <p>
 * Ce contexte ne connait ni les activites ni leur echeance : il ne fait que donner la valeur en vigueur, par defaut
 * ou fixee, que seul son domaine sait dire. L'atelier et le pupitre n'importent donc rien d'ici.
 * </p>
 */
@Component
class DureeMaximaleDActiviteParametree implements MaximumActivityDurations {

  private final ParametrageRepository parametrage;

  DureeMaximaleDActiviteParametree(ParametrageRepository parametrage) {
    this.parametrage = parametrage;
  }

  @Override
  public MaximumActivityDuration current() {
    return new MaximumActivityDuration(parametrage.get().dureeMaxDActivite().value());
  }
}
