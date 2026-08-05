package com.glm.glmback.elementdefabrication.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;

public record Fiche(Titre titre, Description description, Instant dateDeCreation, Instant dateDeModification) {
  public Fiche {
    Assert.notNull("titre", titre);
    Assert.notNull("description", description);
    Assert.notNull("dateDeCreation", dateDeCreation);
    Assert.field("dateDeModification", dateDeModification).afterOrAt(dateDeCreation);
  }

  private Fiche(String titre, String description, Instant dateDeCreation, Instant dateDeModification) {
    this(new Titre(titre), new Description(description), dateDeCreation, dateDeModification);
  }

  public static FicheTitreBuilder builder() {
    return titre ->
      description -> dateDeCreation -> dateDeModification -> new Fiche(titre, description, dateDeCreation, dateDeModification);
  }

  public interface FicheTitreBuilder {
    FicheDescriptionBuilder titre(String titre);
  }

  public interface FicheDescriptionBuilder {
    FicheDateDeCreationBuilder description(String description);
  }

  public interface FicheDateDeCreationBuilder {
    FicheDateDeModificationBuilder dateDeCreation(Instant dateDeCreation);
  }

  public interface FicheDateDeModificationBuilder {
    Fiche dateDeModification(Instant dateDeModification);
  }
}
