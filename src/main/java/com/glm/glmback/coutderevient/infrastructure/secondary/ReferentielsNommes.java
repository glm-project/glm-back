package com.glm.glmback.coutderevient.infrastructure.secondary;

import com.glm.glmback.coutderevient.domain.OperateurId;
import com.glm.glmback.coutderevient.domain.OperateurNomme;
import com.glm.glmback.coutderevient.domain.OperateursNommes;
import com.glm.glmback.coutderevient.domain.PosteDeTravailId;
import com.glm.glmback.coutderevient.domain.PosteNomme;
import com.glm.glmback.coutderevient.domain.PostesNommes;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Repository;

/** Les noms des operateurs et des postes, relus par lots aux tables des referentiels voisins. */
@Repository
class ReferentielsNommes implements OperateursNommes, PostesNommes {

  private final SpringDataOperateursDuCoutRepository operateurs;
  private final SpringDataPostesDuCoutRepository postes;

  ReferentielsNommes(SpringDataOperateursDuCoutRepository operateurs, SpringDataPostesDuCoutRepository postes) {
    this.operateurs = operateurs;
    this.postes = postes;
  }

  @Override
  public List<OperateurNomme> operateurs(Set<OperateurId> ids) {
    return operateurs.findAllById(ids.stream().map(OperateurId::uuid).toList()).stream().map(OperateurDuCoutEntity::toDomain).toList();
  }

  @Override
  public List<PosteNomme> postes(Set<PosteDeTravailId> ids) {
    return postes.findAllById(ids.stream().map(PosteDeTravailId::uuid).toList()).stream().map(PosteDuCoutEntity::toDomain).toList();
  }
}
