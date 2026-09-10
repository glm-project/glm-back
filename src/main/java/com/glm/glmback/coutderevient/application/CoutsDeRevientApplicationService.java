package com.glm.glmback.coutderevient.application;

import com.glm.glmback.coutderevient.domain.CoutDeRevient;
import com.glm.glmback.coutderevient.domain.CoutsDeRevientService;
import com.glm.glmback.coutderevient.domain.ElementId;
import com.glm.glmback.coutderevient.domain.ElementsValorisables;
import com.glm.glmback.coutderevient.domain.OccupationDesOperateurs;
import com.glm.glmback.coutderevient.domain.PresenceDesOperateurs;
import com.glm.glmback.coutderevient.domain.TravailDeLElement;
import com.glm.glmback.shared.time.domain.Clock;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestration de la lecture des couts de revient.
 *
 * <p>
 * Rien a ecrire : le contexte ne fait que relire les journaux de l'atelier. La transaction est en lecture seule, et
 * elle couvre les quatre requetes du meme coup — element, suivis, occupation, presence — pour que le rapport ne
 * melange pas deux etats de la base.
 * </p>
 */
@Service
public class CoutsDeRevientApplicationService {

  private final CoutsDeRevientService coutsDeRevient;

  public CoutsDeRevientApplicationService(
    ElementsValorisables elements,
    TravailDeLElement travaux,
    OccupationDesOperateurs occupations,
    PresenceDesOperateurs presences,
    Clock clock
  ) {
    this.coutsDeRevient = CoutsDeRevientService.builder()
      .elements(elements)
      .travaux(travaux)
      .occupations(occupations)
      .presences(presences)
      .clock(clock);
  }

  /**
   * Le rapport est reserve au gestionnaire : il expose des couts derives des taux horaires des operateurs, que le
   * pupitre n'a aucune raison de voir. {@code ADMIN} n'a aucun acces metier.
   */
  @Secured("ROLE_GESTIONNAIRE")
  @Transactional(readOnly = true)
  public CoutDeRevient rapport(ElementId element) {
    return coutsDeRevient.rapport(element);
  }
}
