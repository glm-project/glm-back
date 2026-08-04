package com.glm.glmback.elementdefabrication.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;

public record Produit(ProduitId id, Nom nom, Fiche fiche) implements ElementDeFabrication {
  public Produit {
    Assert.notNull("id", id);
    Assert.notNull("nom", nom);
    Assert.notNull("fiche", fiche);
  }

  public static ProduitIdBuilder builder() {
    return id ->
      nom ->
        titre ->
          description ->
            dateDeCreation ->
              dateDeModification ->
                new Produit(
                  id,
                  nom,
                  Fiche.builder()
                    .titre(titre)
                    .description(description)
                    .dateDeCreation(dateDeCreation)
                    .dateDeModification(dateDeModification)
                );
  }

  public interface ProduitIdBuilder {
    ProduitNomBuilder id(ProduitId id);
  }

  public interface ProduitNomBuilder {
    ProduitTitreBuilder nom(Nom nom);
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
