package com.glm.glmback.feuilledetemps.domain;

import com.glm.glmback.shared.error.domain.Assert;

/** L'activite entiere, avant son decoupage en portions calendaires. */
public record ActiviteLue(ActiviteId id, EtatDActivite etat, Plage plage) {
  public ActiviteLue {
    Assert.notNull("id de l'activite", id);
    Assert.notNull("etat de l'activite", etat);
    Assert.notNull("plage de l'activite", plage);
  }

  public static IdentiteBuilder builder() {
    return id -> etat -> plage -> new ActiviteLue(id, etat, plage);
  }

  public interface IdentiteBuilder {
    EtatBuilder id(ActiviteId id);
  }

  public interface EtatBuilder {
    PlageBuilder etat(EtatDActivite etat);
  }

  public interface PlageBuilder {
    ActiviteLue plage(Plage plage);
  }
}
