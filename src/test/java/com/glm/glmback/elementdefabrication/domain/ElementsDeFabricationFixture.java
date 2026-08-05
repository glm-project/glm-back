package com.glm.glmback.elementdefabrication.domain;

import java.time.Instant;

public final class ElementsDeFabricationFixture {

  public static final Instant LE_1ER_JANVIER_2026 = Instant.parse("2026-01-01T00:00:00Z");
  public static final Instant LE_15_JANVIER_2026 = Instant.parse("2026-01-15T10:00:00Z");
  public static final Instant LE_20_FEVRIER_2026 = Instant.parse("2026-02-20T14:30:00Z");
  public static final Instant LE_31_MARS_2026 = Instant.parse("2026-03-31T23:59:59Z");

  public static final Annee ANNEE_2026 = new Annee(2026);

  public static final Prefixe PREFIXE_OF = new Prefixe("OF");
  public static final Prefixe PREFIXE_PRD = new Prefixe("PRD");

  public static final Nom OF_2026_000001 = Nom.of(PREFIXE_OF, ANNEE_2026, 1);
  public static final Nom PRD_2026_000001 = Nom.of(PREFIXE_PRD, ANNEE_2026, 1);

  private ElementsDeFabricationFixture() {}

  public static Titre titreAssemblageCarter() {
    return new Titre("Assemblage carter");
  }

  public static Titre titreAssemblageCarterRevise() {
    return new Titre("Assemblage carter revise");
  }

  public static Titre titreCarterMoteur() {
    return new Titre("Carter moteur");
  }

  public static Description descriptionCarterEnFonte() {
    return new Description("Carter en fonte");
  }

  public static Periode premierTrimestre2026() {
    return new Periode(LE_1ER_JANVIER_2026, LE_31_MARS_2026);
  }

  public static ElementDeFabricationCriteria criteresPremierTrimestre2026() {
    return new ElementDeFabricationCriteria(premierTrimestre2026());
  }

  public static ElementDeFabricationToCreate elementDeFabricationToCreateAssemblageCarter() {
    return new ElementDeFabricationToCreate(
      TypeDElementDeFabrication.ORDRE_DE_FABRICATION,
      titreAssemblageCarter(),
      descriptionCarterEnFonte()
    );
  }

  public static ElementDeFabricationToCreate elementDeFabricationToCreateCarterMoteur() {
    return new ElementDeFabricationToCreate(TypeDElementDeFabrication.PRODUIT, titreCarterMoteur(), descriptionCarterEnFonte());
  }

  public static ElementDeFabricationToUpdate elementDeFabricationToUpdateAssemblageCarterRevise(ElementDeFabricationId id) {
    return new ElementDeFabricationToUpdate(id, titreAssemblageCarterRevise(), descriptionCarterEnFonte());
  }

  public static Fiche ficheAssemblageCarter() {
    return Fiche.builder()
      .titre(titreAssemblageCarter().value())
      .description(descriptionCarterEnFonte().value())
      .dateDeCreation(LE_15_JANVIER_2026)
      .dateDeModification(LE_15_JANVIER_2026);
  }

  public static Fiche ficheCarterMoteur() {
    return Fiche.builder()
      .titre(titreCarterMoteur().value())
      .description(descriptionCarterEnFonte().value())
      .dateDeCreation(LE_15_JANVIER_2026)
      .dateDeModification(LE_15_JANVIER_2026);
  }

  public static ElementDeFabrication elementDeFabricationAssemblageCarter() {
    return elementDeFabricationAssemblageCarter(ElementDeFabricationId.newId());
  }

  public static ElementDeFabrication elementDeFabricationAssemblageCarter(ElementDeFabricationId id) {
    return elementDeFabricationAssemblageCarter(id, LE_15_JANVIER_2026);
  }

  public static ElementDeFabrication elementDeFabricationAssemblageCarterCreeLe(Instant dateDeCreation) {
    return elementDeFabricationAssemblageCarter(ElementDeFabricationId.newId(), dateDeCreation);
  }

  public static ElementDeFabrication elementDeFabricationAssemblageCarterRevise(ElementDeFabricationId id) {
    return ElementDeFabrication.builder()
      .id(id)
      .type(TypeDElementDeFabrication.ORDRE_DE_FABRICATION)
      .nom(OF_2026_000001)
      .titre(titreAssemblageCarterRevise().value())
      .description(descriptionCarterEnFonte().value())
      .dateDeCreation(LE_15_JANVIER_2026)
      .dateDeModification(LE_20_FEVRIER_2026);
  }

  public static ElementDeFabrication elementDeFabricationCarterMoteur() {
    return ElementDeFabrication.builder()
      .id(ElementDeFabricationId.newId())
      .type(TypeDElementDeFabrication.PRODUIT)
      .nom(PRD_2026_000001)
      .titre(titreCarterMoteur().value())
      .description(descriptionCarterEnFonte().value())
      .dateDeCreation(LE_15_JANVIER_2026)
      .dateDeModification(LE_15_JANVIER_2026);
  }

  private static ElementDeFabrication elementDeFabricationAssemblageCarter(ElementDeFabricationId id, Instant dateDeCreation) {
    return ElementDeFabrication.builder()
      .id(id)
      .type(TypeDElementDeFabrication.ORDRE_DE_FABRICATION)
      .nom(OF_2026_000001)
      .titre(titreAssemblageCarter().value())
      .description(descriptionCarterEnFonte().value())
      .dateDeCreation(dateDeCreation)
      .dateDeModification(dateDeCreation);
  }
}
