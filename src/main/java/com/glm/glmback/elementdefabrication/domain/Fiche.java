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

  private Fiche(FicheBuilder builder) {
    this(new Titre(builder.titre), new Description(builder.description), builder.dateDeCreation, builder.dateDeModification);
  }

  static FicheTitreBuilder builder() {
    return new FicheBuilder();
  }

  public Fiche revise(Titre titre, Description description, Instant dateDeModification) {
    return new Fiche(titre, description, dateDeCreation, dateDeModification);
  }

  private static final class FicheBuilder
    implements FicheTitreBuilder, FicheDescriptionBuilder, FicheDateDeCreationBuilder, FicheDateDeModificationBuilder
  {

    private String titre;
    private String description;
    private Instant dateDeCreation;
    private Instant dateDeModification;

    @Override
    public FicheDescriptionBuilder titre(String titre) {
      this.titre = titre;

      return this;
    }

    @Override
    public FicheDateDeCreationBuilder description(String description) {
      this.description = description;

      return this;
    }

    @Override
    public FicheDateDeModificationBuilder dateDeCreation(Instant dateDeCreation) {
      this.dateDeCreation = dateDeCreation;

      return this;
    }

    @Override
    public Fiche dateDeModification(Instant dateDeModification) {
      this.dateDeModification = dateDeModification;

      return new Fiche(this);
    }
  }

  interface FicheTitreBuilder {
    FicheDescriptionBuilder titre(String titre);
  }

  interface FicheDescriptionBuilder {
    FicheDateDeCreationBuilder description(String description);
  }

  interface FicheDateDeCreationBuilder {
    FicheDateDeModificationBuilder dateDeCreation(Instant dateDeCreation);
  }

  interface FicheDateDeModificationBuilder {
    Fiche dateDeModification(Instant dateDeModification);
  }
}
