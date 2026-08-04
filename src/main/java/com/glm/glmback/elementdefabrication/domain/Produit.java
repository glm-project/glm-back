package com.glm.glmback.elementdefabrication.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;

public record Produit(ProduitId id, Fiche fiche) implements ElementDeFabrication {
  public Produit {
    Assert.notNull("id", id);
    Assert.notNull("fiche", fiche);
  }

  public static ProduitIdBuilder builder() {
    return id ->
      titre ->
        description ->
          dateDeCreation ->
            dateDeModification ->
              new Produit(
                id,
                Fiche.builder().titre(titre).description(description).dateDeCreation(dateDeCreation).dateDeModification(dateDeModification)
              );
  }

  public interface ProduitIdBuilder {
    ProduitTitreBuilder id(ProduitId id);
  }

  public interface ProduitTitreBuilder {
    ProduitDescriptionBuilder titre(Titre titre);
  }

  public interface ProduitDescriptionBuilder {
    ProduitDateDeCreationBuilder description(Description description);
  }

  public interface ProduitDateDeCreationBuilder {
    ProduitDateDeModificationBuilder dateDeCreation(Instant dateDeCreation);
  }

  public interface ProduitDateDeModificationBuilder {
    Produit dateDeModification(Instant dateDeModification);
  }
}
