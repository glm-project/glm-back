package com.glm.glmback.elementdefabrication.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.Optional;

public record Fiche(Optional<Reference> reference, Optional<Description> description, Instant dateDeCreation, Instant dateDeModification) {
  public Fiche {
    Assert.notNull("reference", reference);
    Assert.notNull("description", description);
    Assert.notNull("dateDeCreation", dateDeCreation);
    Assert.field("dateDeModification", dateDeModification).afterOrAt(dateDeCreation);
  }

  private Fiche(FicheBuilder builder) {
    this(Reference.of(builder.reference), Description.of(builder.description), builder.dateDeCreation, builder.dateDeModification);
  }

  static FicheReferenceBuilder builder() {
    return new FicheBuilder();
  }

  public Fiche revise(Optional<Reference> reference, Optional<Description> description, Instant dateDeModification) {
    return new Fiche(reference, description, dateDeCreation, dateDeModification);
  }

  private static final class FicheBuilder
    implements FicheReferenceBuilder, FicheDescriptionBuilder, FicheDateDeCreationBuilder, FicheDateDeModificationBuilder
  {

    private String reference;
    private String description;
    private Instant dateDeCreation;
    private Instant dateDeModification;

    @Override
    public FicheDescriptionBuilder reference(String reference) {
      this.reference = reference;

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

  interface FicheReferenceBuilder {
    FicheDescriptionBuilder reference(String reference);
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
