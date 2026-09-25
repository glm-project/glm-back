package com.glm.glmback.syntheseheures.infrastructure.secondary;

import com.glm.glmback.syntheseheures.domain.OperateurId;
import com.glm.glmback.syntheseheures.domain.Plage;
import com.glm.glmback.syntheseheures.domain.PointagesDAtelier;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * Le dernier pointage d'OF d'un operateur, lu dans le journal de l'atelier sans importer son code. La periode est
 * toujours bornee : c'est la fenetre de recherche d'une journee abandonnee.
 */
@Repository
class PointagesDeLaSynthese implements PointagesDAtelier {

  private final SpringDataPointagesDAtelierDeLaSyntheseRepository pointages;

  PointagesDeLaSynthese(SpringDataPointagesDAtelierDeLaSyntheseRepository pointages) {
    this.pointages = pointages;
  }

  @Override
  public Optional<Instant> dernierPointage(OperateurId operateur, Plage periode) {
    return pointages
      .findFirstByOperateurIdAndAnnulationDateIsNullAndDateDeSurvenueBetweenOrderByDateDeSurvenueDesc(
        operateur.uuid(),
        periode.debut(),
        periode.fin().orElseThrow()
      )
      .map(PointageDAtelierDeLaSyntheseEntity::dateDeSurvenue);
  }
}
