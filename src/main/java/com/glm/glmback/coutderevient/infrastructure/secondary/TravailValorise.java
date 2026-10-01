package com.glm.glmback.coutderevient.infrastructure.secondary;

import com.glm.glmback.coutderevient.domain.*;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

/** Lecture des projections privees : aucun repli du journal. */
@Repository
class TravailValorise implements TravailDeLElement, OccupationDesOperateurs {

  private final SpringDataActivitesValoriseesRepository activites;

  TravailValorise(SpringDataActivitesValoriseesRepository activites) {
    this.activites = activites;
  }

  @Override
  public List<ActiviteInterpretee> activites(ElementId element) {
    return activites.de(element.uuid()).stream().map(ActiviteValoriseeEntity::toDomain).toList();
  }

  @Override
  public List<ActiviteInterpretee> activites(Set<OperateurId> operateurs, Periode periode) {
    Set<UUID> ids = operateurs.stream().map(OperateurId::uuid).collect(Collectors.toSet());
    return activites.occupation(ids, periode.debut(), periode.fin()).stream().map(ActiviteValoriseeEntity::toDomain).toList();
  }
}
