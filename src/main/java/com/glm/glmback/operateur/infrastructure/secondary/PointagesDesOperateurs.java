package com.glm.glmback.operateur.infrastructure.secondary;

import com.glm.glmback.operateur.domain.OperateurId;
import com.glm.glmback.operateur.domain.OperateursQuiOntPointe;
import org.springframework.stereotype.Repository;

/**
 * Ce qui empeche de supprimer un operateur : tout fait historique du journal d'atelier a son nom.
 */
@Repository
class PointagesDesOperateurs implements OperateursQuiOntPointe {

  private final SpringDataPointagesDesOperateursRepository pointages;

  PointagesDesOperateurs(SpringDataPointagesDesOperateursRepository pointages) {
    this.pointages = pointages;
  }

  @Override
  public boolean aPointe(OperateurId operateur) {
    return pointages.findFirstByOperateurId(operateur.uuid()).isPresent();
  }
}
