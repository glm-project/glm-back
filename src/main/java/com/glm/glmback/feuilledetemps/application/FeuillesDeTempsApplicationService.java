package com.glm.glmback.feuilledetemps.application;

import com.glm.glmback.feuilledetemps.domain.FeuilleDeTemps;
import com.glm.glmback.feuilledetemps.domain.FeuillesDeTempsService;
import com.glm.glmback.feuilledetemps.domain.FuseauHoraireDeLEntreprise;
import com.glm.glmback.feuilledetemps.domain.OperateurId;
import com.glm.glmback.feuilledetemps.domain.OperateursConnus;
import com.glm.glmback.feuilledetemps.domain.PointagesDAtelier;
import com.glm.glmback.feuilledetemps.domain.PresenceDeLOperateur;
import com.glm.glmback.feuilledetemps.domain.SemaineCalendaire;
import com.glm.glmback.feuilledetemps.domain.SeuilDAmplitude;
import com.glm.glmback.shared.time.domain.Clock;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestration de la lecture des feuilles de temps.
 *
 * <p>
 * Rien a ecrire : le contexte ne fait que relire les journaux de l'atelier. La transaction est donc en lecture seule,
 * et elle couvre les deux requetes du meme coup — journees puis journaux — pour que la feuille ne melange pas deux
 * etats de la base.
 * </p>
 */
@Service
public class FeuillesDeTempsApplicationService {

  private final FeuillesDeTempsService feuillesDeTemps;

  public FeuillesDeTempsApplicationService(
    PresenceDeLOperateur presences,
    OperateursConnus operateurs,
    FuseauHoraireDeLEntreprise fuseau,
    SeuilDAmplitude seuil,
    PointagesDAtelier pointages,
    Clock clock
  ) {
    this.feuillesDeTemps = FeuillesDeTempsService.builder()
      .presences(presences)
      .operateurs(operateurs)
      .fuseau(fuseau)
      .seuil(seuil)
      .pointages(pointages)
      .clock(clock);
  }

  /**
   * La lecture est ouverte aux deux roles metier : l'operateur consulte son propre historique, le gestionnaire celui
   * de son atelier. {@code ADMIN} n'a aucun acces metier.
   */
  @Secured({ "ROLE_USER", "ROLE_GESTIONNAIRE" })
  @Transactional(readOnly = true)
  public FeuilleDeTemps historique(OperateurId operateur, SemaineCalendaire semaine) {
    return feuillesDeTemps.historique(operateur, semaine);
  }
}
