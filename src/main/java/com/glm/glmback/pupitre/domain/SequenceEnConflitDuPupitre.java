package com.glm.glmback.pupitre.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.List;
import java.util.Optional;

/** Interpretation en conflit projetee par atelier : aucune de ses activites n'est actionnable. */
public record SequenceEnConflitDuPupitre(CleDActivite cle, List<ActiviteId> activites, List<PointageId> pointages) {
  public SequenceEnConflitDuPupitre {
    Assert.notNull("cle", cle);
    Assert.field("activites", activites).notNull().noNullElement();
    Assert.field("pointages", pointages).notEmpty().noNullElement();
    activites = List.copyOf(activites);
    pointages = List.copyOf(pointages);
  }

  public OperateurId operateur() {
    return cle.operateur();
  }

  public Optional<PosteDeTravailId> poste() {
    return cle.poste();
  }
}
