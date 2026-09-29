package com.glm.glmback.feuilledetemps.application;

import com.glm.glmback.feuilledetemps.domain.ActivitesDeLOperateur;
import com.glm.glmback.feuilledetemps.domain.FeuilleDeTemps;
import com.glm.glmback.feuilledetemps.domain.FeuillesDeTempsService;
import com.glm.glmback.feuilledetemps.domain.FuseauHoraireDeLEntreprise;
import com.glm.glmback.feuilledetemps.domain.OperateurId;
import com.glm.glmback.feuilledetemps.domain.OperateursConnus;
import com.glm.glmback.feuilledetemps.domain.SemaineCalendaire;
import com.glm.glmback.shared.time.domain.Clock;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestration de la lecture des feuilles de temps.
 *
 * <p>
 * La transaction en lecture seule couvre le referentiel, les activites projetees par atelier.
 * L'isolation par defaut ne garantit pas un instantane commun face aux ecritures concurrentes.
 * </p>
 */
@Service
public class FeuillesDeTempsApplicationService {

  private final FeuillesDeTempsService feuillesDeTemps;

  public FeuillesDeTempsApplicationService(
    OperateursConnus operateurs,
    FuseauHoraireDeLEntreprise fuseau,
    ActivitesDeLOperateur activites,
    Clock clock
  ) {
    this.feuillesDeTemps = FeuillesDeTempsService.builder().operateurs(operateurs).fuseau(fuseau).activites(activites).clock(clock);
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
