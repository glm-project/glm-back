package com.glm.glmback.elementdefabrication.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;

public record OrdreDeFabrication(OrdreDeFabricationId id, Nom nom, Fiche fiche) implements ElementDeFabrication {
  public OrdreDeFabrication {
    Assert.notNull("id", id);
    Assert.notNull("nom", nom);
    Assert.notNull("fiche", fiche);
  }

  private OrdreDeFabrication(
    OrdreDeFabricationId id,
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

  public static OrdreDeFabricationIdBuilder builder() {
    return id ->
      prefixe ->
        annee ->
          compteur ->
            titre ->
              description ->
                dateDeCreation ->
                  dateDeModification ->
                    new OrdreDeFabrication(id, prefixe, annee, compteur, titre, description, dateDeCreation, dateDeModification);
  }

  public interface OrdreDeFabricationIdBuilder {
    OrdreDeFabricationPrefixeBuilder id(OrdreDeFabricationId id);
  }

  public interface OrdreDeFabricationPrefixeBuilder {
    OrdreDeFabricationAnneeBuilder prefixe(String prefixe);
  }

  public interface OrdreDeFabricationAnneeBuilder {
    OrdreDeFabricationCompteurBuilder annee(int annee);
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
