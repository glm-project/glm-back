package com.glm.glmback.syntheseheures.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.Optional;

/** L'activite entiere, avant son decoupage en portions calendaires. */
public record ActiviteLue(ActiviteId id, EtatDActivite etat, Plage plage, Optional<Instant> finAuPlusTard) {
  public ActiviteLue {
    Assert.notNull("id de l'activite", id);
    Assert.notNull("etat de l'activite", etat);
    Assert.notNull("plage de l'activite", plage);
    Assert.notNull("fin au plus tard", finAuPlusTard);
  }

  public static IdentiteBuilder builder() {
    return id -> etat -> plage -> finAuPlusTard -> new ActiviteLue(id, etat, plage, finAuPlusTard);
  }

  public interface IdentiteBuilder {
    EtatBuilder id(ActiviteId id);
  }

  public interface EtatBuilder {
    PlageBuilder etat(EtatDActivite etat);
  }

  public interface PlageBuilder {
    FinAuPlusTardBuilder plage(Plage plage);
  }

  public interface FinAuPlusTardBuilder {
    ActiviteLue finAuPlusTard(Optional<Instant> finAuPlusTard);
  }
}
