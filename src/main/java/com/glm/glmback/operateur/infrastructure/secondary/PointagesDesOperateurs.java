package com.glm.glmback.operateur.infrastructure.secondary;

import com.glm.glmback.operateur.domain.OperateurId;
import com.glm.glmback.operateur.domain.OperateursQuiOntPointe;
import org.springframework.stereotype.Repository;

/**
 * Ce qui empeche de supprimer un operateur : le temps deja pointe a son nom, sur un element comme en presence.
 */
@Repository
class PointagesDesOperateurs implements OperateursQuiOntPointe {

  private final SpringDataPointagesDesOperateursRepository pointages;
  private final SpringDataJourneesDesOperateursRepository journees;

  PointagesDesOperateurs(SpringDataPointagesDesOperateursRepository pointages, SpringDataJourneesDesOperateursRepository journees) {
    this.pointages = pointages;
    this.journees = journees;
  }

  @Override
  public boolean aPointe(OperateurId operateur) {
    return (
      pointages.findFirstByOperateurId(operateur.uuid()).isPresent() || journees.findFirstByOperateurId(operateur.uuid()).isPresent()
    );
  }
}
