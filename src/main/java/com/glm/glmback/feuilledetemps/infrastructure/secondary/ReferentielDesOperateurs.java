package com.glm.glmback.feuilledetemps.infrastructure.secondary;

import com.glm.glmback.feuilledetemps.domain.OperateurConnu;
import com.glm.glmback.feuilledetemps.domain.OperateurId;
import com.glm.glmback.feuilledetemps.domain.OperateursConnus;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * Les operateurs declares par l'entreprise courante, tels que la feuille de temps les lit.
 */
@Repository
class ReferentielDesOperateurs implements OperateursConnus {

  private final SpringDataOperateursLectureRepository operateurs;

  ReferentielDesOperateurs(SpringDataOperateursLectureRepository operateurs) {
    this.operateurs = operateurs;
  }

  @Override
  public Optional<OperateurConnu> get(OperateurId id) {
    return operateurs.findById(id.uuid()).map(OperateurLectureEntity::toDomain);
  }
}
