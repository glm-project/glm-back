package com.glm.glmback.coutderevient.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Le 11 mai 2026, une journee de 8 h a 17 h, et de quoi la valoriser.
 *
 * <p>
 * Aucun fuseau horaire ici, contrairement a la fixture de la feuille de temps : ce contexte ne connait que des
 * instants, comme l'atelier dont il relit les activites interpretees.
 * </p>
 */
public final class CoutDeRevientFixture {

  public static final Instant LE_11_MAI_A_8H = Instant.parse("2026-05-11T08:00:00Z");
  public static final Instant LE_11_MAI_A_9H = Instant.parse("2026-05-11T09:00:00Z");
  public static final Instant LE_11_MAI_A_10H = Instant.parse("2026-05-11T10:00:00Z");
  public static final Instant LE_11_MAI_A_11H = Instant.parse("2026-05-11T11:00:00Z");
  public static final Instant LE_11_MAI_A_12H = Instant.parse("2026-05-11T12:00:00Z");
  public static final Instant LE_11_MAI_A_13H = Instant.parse("2026-05-11T13:00:00Z");
  public static final Instant LE_11_MAI_A_14H = Instant.parse("2026-05-11T14:00:00Z");
  public static final Instant LE_11_MAI_A_17H = Instant.parse("2026-05-11T17:00:00Z");
  public static final Instant LE_12_MAI_A_8H = Instant.parse("2026-05-12T08:00:00Z");
  public static final Instant LE_11_MAI_A_15H = Instant.parse("2026-05-11T15:00:00Z");
  public static final Instant LE_11_MAI_A_21H = Instant.parse("2026-05-11T21:00:00Z");
  public static final Instant LE_12_MAI_A_7H = Instant.parse("2026-05-12T07:00:00Z");
  public static final Instant LE_12_MAI_A_9H = Instant.parse("2026-05-12T09:00:00Z");
  public static final Instant LE_12_MAI_A_10H = Instant.parse("2026-05-12T10:00:00Z");
  public static final Instant LE_12_MAI_A_18H = Instant.parse("2026-05-12T18:00:00Z");
  public static final Instant LE_13_MAI_A_8H = Instant.parse("2026-05-13T08:00:00Z");

  public static final ElementId ELEMENT_ID_OF = new ElementId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
  public static final NomDElement NOM_D_ELEMENT_OF_2026_000001 = new NomDElement("OF-2026-000001");
  public static final ElementValorise ELEMENT_VALORISE_OF = new ElementValorise(
    ELEMENT_ID_OF,
    NOM_D_ELEMENT_OF_2026_000001,
    TypeDElement.ORDRE_DE_FABRICATION
  );

  public static final OperateurId OPERATEUR_ID_DUPONT = new OperateurId(UUID.fromString("33333333-3333-3333-3333-333333333333"));
  public static final OperateurId OPERATEUR_ID_MARTIN = new OperateurId(UUID.fromString("44444444-4444-4444-4444-444444444444"));

  public static final PosteDeTravailId POSTE_ID_FRAISEUSE = new PosteDeTravailId(UUID.fromString("55555555-5555-5555-5555-555555555555"));
  public static final PosteDeTravailId POSTE_ID_TOUR = new PosteDeTravailId(UUID.fromString("66666666-6666-6666-6666-666666666666"));

  public static final NatureDOperation NATURE_FRAISAGE = new NatureDOperation("Fraisage");
  public static final NatureDOperation NATURE_TOURNAGE = new NatureDOperation("Tournage");

  public static final CoutHoraire COUT_HORAIRE_DE_45_EUROS = new CoutHoraire(new BigDecimal("45.00"));
  public static final CoutHoraire COUT_HORAIRE_DE_60_EUROS = new CoutHoraire(new BigDecimal("60.00"));
  public static final TauxHoraire TAUX_HORAIRE_DE_20_EUROS = new TauxHoraire(new BigDecimal("20.00"));

  public static final Activite ACTIVITE_FRAISAGE = Activite.builder()
    .operateur(OPERATEUR_ID_DUPONT)
    .element(ELEMENT_ID_OF)
    .poste(Optional.of(POSTE_ID_FRAISEUSE))
    .nature(Optional.of(NATURE_FRAISAGE))
    .coutHoraire(Optional.of(COUT_HORAIRE_DE_45_EUROS))
    .tauxHoraire(Optional.of(TAUX_HORAIRE_DE_20_EUROS))
    .categorie(CategorieDActivite.TRAVAIL);
  public static final Activite ACTIVITE_TOURNAGE = Activite.builder()
    .operateur(OPERATEUR_ID_DUPONT)
    .element(ELEMENT_ID_OF)
    .poste(Optional.of(POSTE_ID_TOUR))
    .nature(Optional.of(NATURE_TOURNAGE))
    .coutHoraire(Optional.of(COUT_HORAIRE_DE_60_EUROS))
    .tauxHoraire(Optional.of(TAUX_HORAIRE_DE_20_EUROS))
    .categorie(CategorieDActivite.TRAVAIL);
  public static final Activite ACTIVITE_NC_FRAISAGE = Activite.builder()
    .operateur(OPERATEUR_ID_DUPONT)
    .element(ELEMENT_ID_OF)
    .poste(Optional.of(POSTE_ID_FRAISEUSE))
    .nature(Optional.of(NATURE_FRAISAGE))
    .coutHoraire(Optional.of(COUT_HORAIRE_DE_45_EUROS))
    .tauxHoraire(Optional.of(TAUX_HORAIRE_DE_20_EUROS))
    .categorie(CategorieDActivite.NON_CONFORMITE);
  public static final Activite ACTIVITE_TOURNAGE_SANS_TAUX = Activite.builder()
    .operateur(OPERATEUR_ID_DUPONT)
    .element(ELEMENT_ID_OF)
    .poste(Optional.of(POSTE_ID_TOUR))
    .nature(Optional.of(NATURE_TOURNAGE))
    .coutHoraire(Optional.of(COUT_HORAIRE_DE_60_EUROS))
    .tauxHoraire(Optional.empty())
    .categorie(CategorieDActivite.TRAVAIL);

  private CoutDeRevientFixture() {}

  public static ActiviteInterpretee activiteInterpreteeDeFraisage(Plage plage) {
    return ActiviteInterpretee.builder()
      .id(new ActiviteId(UUID.randomUUID()))
      .activite(ACTIVITE_FRAISAGE)
      .plage(plage)
      .echeance(plage.debut().plusSeconds(46800))
      .finAuPlusTard(Optional.empty());
  }

  public static ActiviteInterpretee activiteInterpreteeDeTournage(Plage plage) {
    return ActiviteInterpretee.builder()
      .id(new ActiviteId(UUID.randomUUID()))
      .activite(ACTIVITE_TOURNAGE)
      .plage(plage)
      .echeance(plage.debut().plusSeconds(46800))
      .finAuPlusTard(Optional.empty());
  }
}
