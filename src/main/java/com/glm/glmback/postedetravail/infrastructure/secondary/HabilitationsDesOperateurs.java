package com.glm.glmback.postedetravail.infrastructure.secondary;

import com.glm.glmback.postedetravail.domain.PosteDeTravailId;
import com.glm.glmback.postedetravail.domain.PostesEnUsage;
import org.springframework.stereotype.Repository;

/**
 * Ce qui empeche de supprimer un poste : les operateurs de l'entreprise courante qui y sont habilites.
 */
@Repository
class HabilitationsDesOperateurs implements PostesEnUsage {

  private final SpringDataHabilitationsRepository habilitations;

  HabilitationsDesOperateurs(SpringDataHabilitationsRepository habilitations) {
    this.habilitations = habilitations;
  }

  @Override
  public boolean estHabilite(PosteDeTravailId poste) {
    return habilitations.findFirstByPosteId(poste.uuid()).isPresent();
  }
}
