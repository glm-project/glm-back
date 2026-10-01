package com.glm.glmback.pupitre.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.List;

/** Les activites interpretees par l'atelier et l'existence de pointages encore actifs. */
public record SituationDuSuivi(List<ActivitePointable> activites, boolean aDesPointages) {
  public SituationDuSuivi {
    Assert.field("activites", activites).notNull().noNullElement();
    activites = List.copyOf(activites);
  }

  public EtatDuSuivi etat() {
    if (!activites.isEmpty()) {
      return EtatDuSuivi.EN_COURS;
    }
    return aDesPointages ? EtatDuSuivi.INTERROMPU : EtatDuSuivi.EN_ATTENTE;
  }
}
