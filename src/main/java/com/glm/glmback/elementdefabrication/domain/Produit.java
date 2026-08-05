package com.glm.glmback.elementdefabrication.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;

public record Produit(ElementDeFabricationId id, Nom nom, Fiche fiche) implements ElementDeFabrication {
  public Produit {
    Assert.notNull("id", id);
    Assert.notNull("nom", nom);
    Assert.notNull("fiche", fiche);
  }

  private Produit(
    ElementDeFabricationId id,
    String prefixe,
    int annee,
    long compteur,
    String titre,
    String description,
    Instant dateDeCreation,
    Instant dateDeModification
  ) {
    this(
      id,
      Nom.of(new Prefixe(prefixe), new Annee(annee), compteur),
      Fiche.builder().titre(titre).description(description).dateDeCreation(dateDeCreation).dateDeModification(dateDeModification)
    );
  }

  public static ProduitIdBuilder builder() {
    return id ->
      prefixe ->
        annee ->
          compteur ->
            titre ->
              description ->
                dateDeCreation ->
                  dateDeModification -> new Produit(id, prefixe, annee, compteur, titre, description, dateDeCreation, dateDeModification);
  }

  public interface ProduitIdBuilder {
    ProduitPrefixeBuilder id(ElementDeFabricationId id);
  }

  public interface ProduitPrefixeBuilder {
    ProduitAnneeBuilder prefixe(String prefixe);
  }

  public interface ProduitAnneeBuilder {
    ProduitCompteurBuilder annee(int annee);
  }

  public interface ProduitCompteurBuilder {
    ProduitTitreBuilder compteur(long compteur);
  }

  public interface ProduitTitreBuilder {
    ProduitDescriptionBuilder titre(String titre);
  }

  public interface ProduitDescriptionBuilder {
    ProduitDateDeCreationBuilder description(String description);
  }

  public interface ProduitDateDeCreationBuilder {
    ProduitDateDeModificationBuilder dateDeCreation(Instant dateDeCreation);
  }

  public interface ProduitDateDeModificationBuilder {
    Produit dateDeModification(Instant dateDeModification);
  }
}
