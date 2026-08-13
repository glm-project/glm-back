package com.glm.glmback.atelier.infrastructure.secondary;

import com.glm.glmback.atelier.domain.OperateurConnu;
import com.glm.glmback.atelier.domain.OperateurId;
import com.glm.glmback.atelier.domain.OperateursConnus;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

/**
 * Les operateurs declares par l'entreprise courante, tels que l'atelier les lit.
 */
@Repository
class OperateursDuReferentiel implements OperateursConnus {

  private final SpringDataOperateursConnusRepository operateurs;

  OperateursDuReferentiel(SpringDataOperateursConnusRepository operateurs) {
    this.operateurs = operateurs;
  }

  @Override
  public boolean existe(OperateurId id) {
    return operateurs.existsById(id.uuid());
  }

  @Override
  public List<OperateurConnu> parIds(Set<OperateurId> ids) {
    return operateurs
      .findByIdIn(ids.stream().map(OperateurId::uuid).collect(Collectors.toSet()))
      .stream()
      .map(OperateurConnuEntity::toDomain)
      .toList();
  }
}
