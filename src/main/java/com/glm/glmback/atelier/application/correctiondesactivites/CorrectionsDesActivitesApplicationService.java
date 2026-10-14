package com.glm.glmback.atelier.application.correctiondesactivites;

import com.glm.glmback.atelier.domain.Habilitations;
import com.glm.glmback.atelier.domain.OperateursConnus;
import com.glm.glmback.atelier.domain.PostesConnus;
import com.glm.glmback.atelier.domain.SuiviDAtelierRepository;
import com.glm.glmback.atelier.domain.correctiondesactivites.AjoutDUneActivite;
import com.glm.glmback.atelier.domain.correctiondesactivites.ChangementDePosteAEnregistrer;
import com.glm.glmback.atelier.domain.correctiondesactivites.CorrectionTraitee;
import com.glm.glmback.atelier.domain.correctiondesactivites.CorrectionsDesActivitesService;
import com.glm.glmback.atelier.domain.correctiondesactivites.RetraitAEnregistrer;
import com.glm.glmback.shared.activityduration.domain.MaximumActivityDurations;
import com.glm.glmback.shared.time.domain.Clock;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestration des corrections que le gestionnaire apporte a la journee d'un operateur. Les trois sont reservees au
 * gestionnaire : le pupitre ne corrige rien.
 *
 * <p>
 * Chaque correction s'ecrit dans sa transaction. Une saisie perimee (un pointage s'est glisse entre la lecture du suivi
 * et son ecriture) est refusee par la revision du suivi, <code>saisie-concurrente</code> : le client relit la journee,
 * le serveur ne rejoue rien.
 * </p>
 */
@Service
public class CorrectionsDesActivitesApplicationService {

  private final CorrectionsDesActivitesService corrections;

  public CorrectionsDesActivitesApplicationService(
    SuiviDAtelierRepository repository,
    OperateursConnus operateurs,
    PostesConnus postes,
    Habilitations habilitations,
    MaximumActivityDurations dureesMax,
    Clock clock
  ) {
    this.corrections = CorrectionsDesActivitesService.builder()
      .repository(repository)
      .operateurs(operateurs)
      .postes(postes)
      .habilitations(habilitations)
      .dureeMax(dureesMax)
      .clock(clock);
  }

  @Secured("ROLE_GESTIONNAIRE")
  @Transactional
  public CorrectionTraitee ajoute(AjoutDUneActivite commande) {
    return corrections.ajoute(commande);
  }

  @Secured("ROLE_GESTIONNAIRE")
  @Transactional
  public CorrectionTraitee changeLePoste(ChangementDePosteAEnregistrer commande) {
    return corrections.changeLePoste(commande);
  }

  @Secured("ROLE_GESTIONNAIRE")
  @Transactional
  public CorrectionTraitee retire(RetraitAEnregistrer commande) {
    return corrections.retire(commande);
  }
}
