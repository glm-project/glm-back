package com.glm.glmback.feuilledetemps.infrastructure.secondary;

import com.glm.glmback.feuilledetemps.domain.ActiviteInterpretee;
import com.glm.glmback.feuilledetemps.domain.ActivitesDeLOperateur;
import com.glm.glmback.feuilledetemps.domain.OperateurId;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Repository;

/** Une requete par operateur et periode, y compris pour une activite commencee avant la semaine. */
@Repository
class ActivitesDeLaFeuilleDeTemps implements ActivitesDeLOperateur {

  private final SpringDataActivitesDeLaFeuilleDeTempsRepository activites;

  ActivitesDeLaFeuilleDeTemps(SpringDataActivitesDeLaFeuilleDeTempsRepository activites) {
    this.activites = activites;
  }

  @Override
  public List<ActiviteInterpretee> recouvrant(OperateurId operateur, Instant debut, Instant finExclusive) {
    return activites.recouvrant(operateur.uuid(), debut, finExclusive).stream().map(ActiviteDeLaFeuilleDeTempsEntity::toDomain).toList();
  }
}
