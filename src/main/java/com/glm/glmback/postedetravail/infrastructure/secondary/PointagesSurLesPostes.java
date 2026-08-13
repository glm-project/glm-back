package com.glm.glmback.postedetravail.infrastructure.secondary;

import com.glm.glmback.postedetravail.domain.PosteDeTravailId;
import com.glm.glmback.postedetravail.domain.PostesPointes;
import org.springframework.stereotype.Repository;

/**
 * Ce qui empeche de supprimer un poste, une fois les habilitations retirees : le temps deja pointe dessus.
 */
@Repository
class PointagesSurLesPostes implements PostesPointes {

  private final SpringDataPostesPointesRepository pointages;

  PointagesSurLesPostes(SpringDataPostesPointesRepository pointages) {
    this.pointages = pointages;
  }

  @Override
  public boolean aServiAPointer(PosteDeTravailId poste) {
    return pointages.findFirstByPosteId(poste.uuid()).isPresent();
  }
}
