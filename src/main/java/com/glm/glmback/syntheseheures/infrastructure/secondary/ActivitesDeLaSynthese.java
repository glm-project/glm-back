package com.glm.glmback.syntheseheures.infrastructure.secondary;

import com.glm.glmback.syntheseheures.domain.ActiviteDElement;
import com.glm.glmback.syntheseheures.domain.ActivitesDeLOperateur;
import com.glm.glmback.syntheseheures.domain.OperateurId;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Repository;

/** Une requete par operateur et periode, y compris pour une activite commencee avant la semaine. */
@Repository
class ActivitesDeLaSynthese implements ActivitesDeLOperateur {

  private final SpringDataActivitesDeLaSyntheseRepository activites;

  ActivitesDeLaSynthese(SpringDataActivitesDeLaSyntheseRepository activites) {
    this.activites = activites;
  }

  @Override
  public List<ActiviteDElement> recouvrant(OperateurId operateur, Instant debut, Instant finExclusive) {
    return activites.recouvrant(operateur.uuid(), debut, finExclusive).stream().map(ActiviteDeLaSyntheseEntity::toDomain).toList();
  }
}
