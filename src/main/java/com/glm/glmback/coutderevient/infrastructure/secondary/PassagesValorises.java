package com.glm.glmback.coutderevient.infrastructure.secondary;

import com.glm.glmback.coutderevient.domain.ElementId;
import com.glm.glmback.coutderevient.domain.PassageEnAtelier;
import com.glm.glmback.coutderevient.domain.PassagesEnAtelier;
import java.util.List;
import org.springframework.stereotype.Repository;

/** Les suivis d'atelier de l'element, relus sans importer le domaine de l'atelier. */
@Repository
class PassagesValorises implements PassagesEnAtelier {

  private final SpringDataSuivisValorisesRepository suivis;

  PassagesValorises(SpringDataSuivisValorisesRepository suivis) {
    this.suivis = suivis;
  }

  @Override
  public List<PassageEnAtelier> passages(ElementId element) {
    return suivis.findByElementId(element.uuid()).stream().map(SuiviValoriseEntity::passage).toList();
  }
}
