package com.glm.glmback.syntheseheures.infrastructure.secondary;

import com.glm.glmback.syntheseheures.domain.OperateurConnu;
import com.glm.glmback.syntheseheures.domain.OperateurId;
import com.glm.glmback.syntheseheures.domain.OperateursConnus;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * Les operateurs declares par l'entreprise courante, tels que la synthese des heures les lit.
 */
@Repository
class OperateursDeLaSynthese implements OperateursConnus {

  private final SpringDataOperateursSyntheseRepository operateurs;

  OperateursDeLaSynthese(SpringDataOperateursSyntheseRepository operateurs) {
    this.operateurs = operateurs;
  }

  @Override
  public Optional<OperateurConnu> get(OperateurId id) {
    return operateurs.findById(id.uuid()).map(OperateurSyntheseEntity::toDomain);
  }
}
