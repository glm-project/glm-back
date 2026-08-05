package com.glm.glmback.elementdefabrication.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;

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
        .titre(builder.titre)
        .description(builder.description)
        .dateDeCreation(builder.dateDeCreation)
        .dateDeModification(builder.dateDeModification)
    );
  }

  static ElementDeFabricationIdBuilder builder() {
    return new ElementDeFabricationBuilder();
  }

  public ElementDeFabrication revise(Titre titre, Description description, Instant dateDeModification) {
    return new ElementDeFabrication(id, type, nom, fiche.revise(titre, description, dateDeModification));
  }

  public Titre titre() {
    return fiche.titre();
  }

  public Description description() {
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
      ElementDeFabricationTitreBuilder,
      ElementDeFabricationDescriptionBuilder,
      ElementDeFabricationDateDeCreationBuilder,
      ElementDeFabricationDateDeModificationBuilder
  {

    private ElementDeFabricationId id;
    private TypeDElementDeFabrication type;
    private Nom nom;
    private String titre;
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
    public ElementDeFabricationTitreBuilder nom(Nom nom) {
      this.nom = nom;

      return this;
    }

    @Override
    public ElementDeFabricationDescriptionBuilder titre(String titre) {
      this.titre = titre;

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

  interface ElementDeFabricationIdBuilder {
    ElementDeFabricationTypeBuilder id(ElementDeFabricationId id);
  }

  interface ElementDeFabricationTypeBuilder {
    ElementDeFabricationNomBuilder type(TypeDElementDeFabrication type);
  }

  interface ElementDeFabricationNomBuilder {
    ElementDeFabricationTitreBuilder nom(Nom nom);
  }

  interface ElementDeFabricationTitreBuilder {
    ElementDeFabricationDescriptionBuilder titre(String titre);
  }

  interface ElementDeFabricationDescriptionBuilder {
    ElementDeFabricationDateDeCreationBuilder description(String description);
  }

  interface ElementDeFabricationDateDeCreationBuilder {
    ElementDeFabricationDateDeModificationBuilder dateDeCreation(Instant dateDeCreation);
  }

  interface ElementDeFabricationDateDeModificationBuilder {
    ElementDeFabrication dateDeModification(Instant dateDeModification);
  }
}
