package com.glm.glmback.elementdefabrication.domain;

import com.glm.glmback.shared.error.domain.Assert;

import java.time.Instant;

public record OrdreDeFabrication(ElementDeFabricationId id, Nom nom, Fiche fiche) implements ElementDeFabrication {
  public OrdreDeFabrication {
    Assert.notNull("id", id);
    Assert.notNull("nom", nom);
    Assert.notNull("fiche", fiche);
  }

  private OrdreDeFabrication(OrdreDeFabricationBuilder builder) {
    this(builder.id,
    Nom.of(builder.prefixe, Annee.of(builder.dateDeCreation), builder.compteur),
    Fiche.builder().titre(builder.titre).description(builder.description).dateDeCreation(builder.dateDeCreation).dateDeModification(builder.dateDeModification));
  }

  public static OrdreDeFabricationIdBuilder builder() {
    return new OrdreDeFabricationBuilder();
  }

  public static class OrdreDeFabricationBuilder implements OrdreDeFabricationIdBuilder, OrdreDeFabricationPrefixeBuilder, OrdreDeFabricationCompteurBuilder, OrdreDeFabricationTitreBuilder, OrdreDeFabricationDescriptionBuilder, OrdreDeFabricationDateDeCreationBuilder, OrdreDeFabricationDateDeModificationBuilder {

    long compteur;
    Instant dateDeCreation;
    String description;
    ElementDeFabricationId id;
    Prefixe prefixe;
    String titre;
    Instant dateDeModification;

    @Override
    public OrdreDeFabricationTitreBuilder compteur(long compteur) {
      this.compteur = compteur;

      return this;
    }

    @Override
    public OrdreDeFabricationDateDeModificationBuilder dateDeCreation(Instant dateDeCreation) {
      this.dateDeCreation = dateDeCreation;

      return this;
    }

    @Override
    public OrdreDeFabricationDateDeCreationBuilder description(String description) {
      this.description = description;

      return this;
    }

    @Override
    public OrdreDeFabricationPrefixeBuilder id(ElementDeFabricationId id) {
      this.id = id;

      return this;
    }

    @Override
    public OrdreDeFabricationCompteurBuilder prefixe(Prefixe prefixe) {
      this.prefixe = prefixe;

      return this;
    }

    @Override
    public OrdreDeFabricationDescriptionBuilder titre(String titre) {
      this.titre = titre;

      return this;
    }

    @Override
    public OrdreDeFabrication dateDeModification(Instant dateDeModification) {
      this.dateDeModification = dateDeModification;

      return new OrdreDeFabrication(this);
    }
  }

  public interface OrdreDeFabricationIdBuilder {
    OrdreDeFabricationPrefixeBuilder id(ElementDeFabricationId id);
  }

  public interface OrdreDeFabricationPrefixeBuilder {
    OrdreDeFabricationCompteurBuilder prefixe(Prefixe prefixe);
  }

  public interface OrdreDeFabricationCompteurBuilder {
    OrdreDeFabricationTitreBuilder compteur(long compteur);
  }

  public interface OrdreDeFabricationTitreBuilder {
    OrdreDeFabricationDescriptionBuilder titre(String titre);
  }

  public interface OrdreDeFabricationDescriptionBuilder {
    OrdreDeFabricationDateDeCreationBuilder description(String description);
  }

  public interface OrdreDeFabricationDateDeCreationBuilder {
    OrdreDeFabricationDateDeModificationBuilder dateDeCreation(Instant dateDeCreation);
  }

  public interface OrdreDeFabricationDateDeModificationBuilder {
    OrdreDeFabrication dateDeModification(Instant dateDeModification);
  }
}
