package com.glm.glmback.atelier.infrastructure.secondary;

import com.glm.glmback.atelier.domain.Habilitations;
import com.glm.glmback.atelier.domain.OperateurId;
import com.glm.glmback.atelier.domain.PosteDeTravailId;
import org.springframework.stereotype.Repository;

/**
 * Les habilitations declarees par l'entreprise courante, telles que l'atelier les lit.
 */
@Repository
class HabilitationsDuReferentiel implements Habilitations {

  private final SpringDataHabilitationsDAtelierRepository habilitations;

  HabilitationsDuReferentiel(SpringDataHabilitationsDAtelierRepository habilitations) {
    this.habilitations = habilitations;
  }

  @Override
  public boolean estHabilite(OperateurId operateur, PosteDeTravailId poste) {
    return habilitations.findFirstByOperateurIdAndPosteId(operateur.uuid(), poste.uuid()).isPresent();
  }
}
