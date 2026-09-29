package com.glm.glmback.feuilledetemps.domain;

import com.glm.glmback.shared.error.domain.Assert;

/** L'activite entiere, avant son decoupage en portions calendaires. */
public record ActiviteLue(ActiviteId id, EtatDActivite etat, Plage plage) {
  public ActiviteLue {
    Assert.notNull("id de l'activite", id);
    Assert.notNull("etat de l'activite", etat);
    Assert.notNull("plage de l'activite", plage);
  }
}
