package com.glm.glmback.elementdefabrication.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.Optional;

public record ElementDeFabrication(ElementDeFabricationId id, TypeDElementDeFabrication type, Nom nom, Fiche fiche) {
  public ElementDeFabrication {
    Assert.notNull("id", id);
    Assert.notNull("type", type);
    Assert.notNull("nom", nom);
    Assert.notNull("fiche", fiche);
  }

  private ElementDeFabrication(ElementDeFabricationBuilder builder) {
    this(
      builder.id,
      builder.type,
      builder.nom,
      Fiche.builder()
        .reference(builder.reference)
        .description(builder.description)
        .dateDeCreation(builder.dateDeCreation)
        .dateDeModification(builder.dateDeModification)
    );
  }

  public static ElementDeFabricationIdBuilder builder() {
    return new ElementDeFabricationBuilder();
  }

  public ElementDeFabrication revise(Optional<Reference> reference, Optional<Description> description, Instant dateDeModification) {
    return new ElementDeFabrication(id, type, nom, fiche.revise(reference, description, dateDeModification));
  }

  public Optional<Reference> reference() {
    return fiche.reference();
  }

  public Optional<Description> description() {
    return fiche.description();
  }

  public Instant dateDeCreation() {
    return fiche.dateDeCreation();
  }

  public Instant dateDeModification() {
    return fiche.dateDeModification();
  }

  private static final class ElementDeFabricationBuilder
    implements
      ElementDeFabricationIdBuilder,
      ElementDeFabricationTypeBuilder,
      ElementDeFabricationNomBuilder,
      ElementDeFabricationReferenceBuilder,
      ElementDeFabricationDescriptionBuilder,
      ElementDeFabricationDateDeCreationBuilder,
      ElementDeFabricationDateDeModificationBuilder
  {

    private ElementDeFabricationId id;
    private TypeDElementDeFabrication type;
    private Nom nom;
    private String reference;
    private String description;
    private Instant dateDeCreation;
    private Instant dateDeModification;

    @Override
    public ElementDeFabricationTypeBuilder id(ElementDeFabricationId id) {
      this.id = id;

      return this;
    }

    @Override
    public ElementDeFabricationNomBuilder type(TypeDElementDeFabrication type) {
      this.type = type;

      return this;
    }

    @Override
    public ElementDeFabricationReferenceBuilder nom(Nom nom) {
      this.nom = nom;

      return this;
    }

    @Override
    public ElementDeFabricationDescriptionBuilder reference(String reference) {
      this.reference = reference;

      return this;
    }

    @Override
    public ElementDeFabricationDateDeCreationBuilder description(String description) {
      this.description = description;

      return this;
    }

    @Override
    public ElementDeFabricationDateDeModificationBuilder dateDeCreation(Instant dateDeCreation) {
      this.dateDeCreation = dateDeCreation;

      return this;
    }

    @Override
    public ElementDeFabrication dateDeModification(Instant dateDeModification) {
      this.dateDeModification = dateDeModification;

      return new ElementDeFabrication(this);
    }
  }

  public interface ElementDeFabricationIdBuilder {
    ElementDeFabricationTypeBuilder id(ElementDeFabricationId id);
  }

  public interface ElementDeFabricationTypeBuilder {
    ElementDeFabricationNomBuilder type(TypeDElementDeFabrication type);
  }

  public interface ElementDeFabricationNomBuilder {
    ElementDeFabricationReferenceBuilder nom(Nom nom);
  }

  public interface ElementDeFabricationReferenceBuilder {
    ElementDeFabricationDescriptionBuilder reference(String reference);
  }

  public interface ElementDeFabricationDescriptionBuilder {
    ElementDeFabricationDateDeCreationBuilder description(String description);
  }

  public interface ElementDeFabricationDateDeCreationBuilder {
    ElementDeFabricationDateDeModificationBuilder dateDeCreation(Instant dateDeCreation);
  }

  public interface ElementDeFabricationDateDeModificationBuilder {
    ElementDeFabrication dateDeModification(Instant dateDeModification);
  }
}
