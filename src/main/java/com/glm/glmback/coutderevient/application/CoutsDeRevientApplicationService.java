package com.glm.glmback.coutderevient.application;

import com.glm.glmback.coutderevient.domain.ConflitsDuCout;
import com.glm.glmback.coutderevient.domain.CoutDeRevient;
import com.glm.glmback.coutderevient.domain.CoutsDeRevientService;
import com.glm.glmback.coutderevient.domain.ElementId;
import com.glm.glmback.coutderevient.domain.ElementsValorisables;
import com.glm.glmback.coutderevient.domain.OccupationDesOperateurs;
import com.glm.glmback.coutderevient.domain.TravailDeLElement;
import com.glm.glmback.shared.time.domain.Clock;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestration de la lecture des couts de revient.
 *
 * <p>
 * Rien a ecrire : le contexte lit les projections de l'atelier dans une transaction en lecture seule.
 * </p>
 */
@Service
public class CoutsDeRevientApplicationService {

  private final CoutsDeRevientService coutsDeRevient;

  public CoutsDeRevientApplicationService(
    ElementsValorisables elements,
    TravailDeLElement travaux,
    OccupationDesOperateurs occupations,
    ConflitsDuCout conflits,
    Clock clock
  ) {
    this.coutsDeRevient = CoutsDeRevientService.builder()
      .elements(elements)
      .travaux(travaux)
      .occupations(occupations)
      .conflits(conflits)
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
