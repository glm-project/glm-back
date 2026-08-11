package com.glm.glmback.elementdefabrication.domain;

import java.time.Instant;
import java.util.Optional;

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

  public static Reference reference1015() {
    return new Reference("1015");
  }

  public static Reference reference1017() {
    return new Reference("1017");
  }

  public static Reference reference2456() {
    return new Reference("2456");
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

  public static ElementDeFabricationToCreate elementDeFabricationToCreateOrdre1015() {
    return new ElementDeFabricationToCreate(TypeDElementDeFabrication.ORDRE_DE_FABRICATION, "1015", "Carter en fonte");
  }

  public static ElementDeFabricationToCreate elementDeFabricationToCreateProduit2456() {
    return new ElementDeFabricationToCreate(TypeDElementDeFabrication.PRODUIT, "2456", "Carter en fonte");
  }

  public static ElementDeFabricationToCreate elementDeFabricationToCreateProduit1015() {
    return new ElementDeFabricationToCreate(TypeDElementDeFabrication.PRODUIT, "1015", "Carter en fonte");
  }

  public static ElementDeFabricationToCreate elementDeFabricationToCreateProduitSansReference() {
    return new ElementDeFabricationToCreate(TypeDElementDeFabrication.PRODUIT, Optional.empty(), Optional.empty());
  }

  public static ElementDeFabricationToUpdate elementDeFabricationToUpdate1017(ElementDeFabricationId id) {
    return new ElementDeFabricationToUpdate(id, "1017", "Carter en fonte");
  }

  public static ElementDeFabricationToUpdate elementDeFabricationToUpdate1015(ElementDeFabricationId id) {
    return new ElementDeFabricationToUpdate(id, "1015", "Carter en fonte");
  }

  public static Fiche fiche1015() {
    return Fiche.builder()
      .reference("1015")
      .description("Carter en fonte")
      .dateDeCreation(LE_15_JANVIER_2026)
      .dateDeModification(LE_15_JANVIER_2026);
  }

  public static Fiche fiche2456() {
    return Fiche.builder()
      .reference("2456")
      .description("Carter en fonte")
      .dateDeCreation(LE_15_JANVIER_2026)
      .dateDeModification(LE_15_JANVIER_2026);
  }

  public static ElementDeFabrication elementDeFabricationOrdre1015() {
    return elementDeFabricationOrdre1015(ElementDeFabricationId.newId());
  }

  public static ElementDeFabrication elementDeFabricationOrdre1015(ElementDeFabricationId id) {
    return elementDeFabricationOrdre1015(id, LE_15_JANVIER_2026);
  }

  public static ElementDeFabrication elementDeFabricationOrdre1015CreeLe(Instant dateDeCreation) {
    return elementDeFabricationOrdre1015(ElementDeFabricationId.newId(), dateDeCreation);
  }

  public static ElementDeFabrication elementDeFabricationOrdre1017(ElementDeFabricationId id) {
    return ElementDeFabrication.builder()
      .id(id)
      .type(TypeDElementDeFabrication.ORDRE_DE_FABRICATION)
      .nom(OF_2026_000001)
      .reference("1017")
      .description("Carter en fonte")
      .dateDeCreation(LE_15_JANVIER_2026)
      .dateDeModification(LE_20_FEVRIER_2026);
  }

  public static ElementDeFabrication elementDeFabricationProduit2456() {
    return ElementDeFabrication.builder()
      .id(ElementDeFabricationId.newId())
      .type(TypeDElementDeFabrication.PRODUIT)
      .nom(PRD_2026_000001)
      .reference("2456")
      .description("Carter en fonte")
      .dateDeCreation(LE_15_JANVIER_2026)
      .dateDeModification(LE_15_JANVIER_2026);
  }

  public static ElementDeFabrication elementDeFabricationProduitSansReference() {
    return ElementDeFabrication.builder()
      .id(ElementDeFabricationId.newId())
      .type(TypeDElementDeFabrication.PRODUIT)
      .nom(PRD_2026_000001)
      .reference(null)
      .description(null)
      .dateDeCreation(LE_15_JANVIER_2026)
      .dateDeModification(LE_15_JANVIER_2026);
  }

  private static ElementDeFabrication elementDeFabricationOrdre1015(ElementDeFabricationId id, Instant dateDeCreation) {
    return ElementDeFabrication.builder()
      .id(id)
      .type(TypeDElementDeFabrication.ORDRE_DE_FABRICATION)
      .nom(OF_2026_000001)
      .reference("1015")
      .description("Carter en fonte")
      .dateDeCreation(dateDeCreation)
      .dateDeModification(dateDeCreation);
  }
}
