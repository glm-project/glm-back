package com.glm.glmback.syntheseheures.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

/**
 * La semaine 20 de 2026, du lundi 11 au dimanche 17 mai, vue de Paris en heure d'ete.
 */
public final class SyntheseHeuresFixture {

  public static final ZoneId ZONE_PARIS = ZoneId.of("Europe/Paris");
  public static final SemaineCalendaire SEMAINE_20_DE_2026 = new SemaineCalendaire(2026, 20);

  public static final LocalDate LUNDI_11_MAI_2026 = LocalDate.of(2026, 5, 11);
  public static final LocalDate MARDI_12_MAI_2026 = LocalDate.of(2026, 5, 12);
  public static final LocalDate MERCREDI_13_MAI_2026 = LocalDate.of(2026, 5, 13);

  public static final SemaineCalendaire SEMAINE_19_DE_2026 = new SemaineCalendaire(2026, 19);
  public static final LocalDate DIMANCHE_10_MAI_2026 = LocalDate.of(2026, 5, 10);
  public static final Instant LE_DIMANCHE_10_MAI_2026_A_20H = aParis(10, 20);
  public static final Instant LE_LUNDI_11_MAI_2026_A_7H = aParis(11, 7);
  public static final Instant LE_LUNDI_11_MAI_2026_A_16H = aParis(11, 16);
  public static final Instant LE_LUNDI_11_MAI_2026_A_20H = aParis(11, 20);
  public static final Instant LE_LUNDI_11_MAI_2026_A_20H05 = aParis(11, 20).plusSeconds(300);
  public static final Instant LE_MARDI_12_MAI_2026_A_10H = aParis(12, 10);
  public static final Instant LE_MARDI_12_MAI_2026_A_20H = aParis(12, 20);
  public static final Instant LE_DIMANCHE_10_MAI_2026_A_8H = aParis(10, 8);
  public static final Instant LE_DIMANCHE_10_MAI_2026_A_17H = aParis(10, 17);
  public static final Instant LE_LUNDI_11_MAI_2026_A_8H = aParis(11, 8);
  public static final Instant LE_LUNDI_11_MAI_2026_A_12H = aParis(11, 12);
  public static final Instant LE_LUNDI_11_MAI_2026_A_13H = aParis(11, 13);
  public static final Instant LE_LUNDI_11_MAI_2026_A_17H = aParis(11, 17);
  public static final Instant LE_LUNDI_11_MAI_2026_A_22H = aParis(11, 22);
  public static final Instant LE_MARDI_12_MAI_2026_A_MINUIT = aParis(12, 0);
  public static final Instant LE_MARDI_12_MAI_2026_A_2H = aParis(12, 2);
  public static final Instant LE_MARDI_12_MAI_2026_A_7H = aParis(12, 7);
  public static final Instant LE_MARDI_12_MAI_2026_A_8H = aParis(12, 8);
  public static final Instant LE_MERCREDI_13_MAI_2026_A_12H = aParis(13, 12);
  public static final Instant LE_LUNDI_11_MAI_2026_A_9H = aParis(11, 9);
  public static final Instant LE_LUNDI_11_MAI_2026_A_10H = aParis(11, 10);
  public static final Instant LE_LUNDI_11_MAI_2026_A_11H = aParis(11, 11);
  public static final Instant LE_LUNDI_11_MAI_2026_A_18H = aParis(11, 18);
  public static final Instant LE_LUNDI_11_MAI_2026_A_23H = aParis(11, 23);
  public static final Instant LE_MARDI_12_MAI_2026_A_1H = aParis(12, 1);
  public static final Instant LE_MARDI_12_MAI_2026_A_9H = aParis(12, 9);
  public static final ElementId ELEMENT_ID_CARTER = new ElementId(UUID.fromString("55555555-5555-5555-5555-555555555555"));
  public static final ElementId ELEMENT_ID_BRIDE = new ElementId(UUID.fromString("66666666-6666-6666-6666-666666666666"));
  public static final PosteDeTravailId POSTE_ID_DMU_50 = new PosteDeTravailId(UUID.fromString("77777777-7777-7777-7777-777777777777"));
  public static final PosteDeTravailId POSTE_ID_TOUR = new PosteDeTravailId(UUID.fromString("88888888-8888-8888-8888-888888888888"));
  public static final NatureDOperation NATURE_FRAISAGE = new NatureDOperation("Fraisage");
  public static final NatureDOperation NATURE_TOURNAGE = new NatureDOperation("Tournage");

  public static final NomDElement NOM_PRD_2026_000015 = new NomDElement("PRD-2026-000015");
  public static final NomDElement NOM_OF_2026_000007 = new NomDElement("OF-2026-000007");
  public static final ElementEngage ELEMENT_ENGAGE_CARTER = new ElementEngage(ELEMENT_ID_CARTER, NOM_PRD_2026_000015, TypeDElement.PRODUIT);
  public static final ElementEngage ELEMENT_ENGAGE_BRIDE = new ElementEngage(
    ELEMENT_ID_BRIDE,
    NOM_OF_2026_000007,
    TypeDElement.ORDRE_DE_FABRICATION
  );
  public static final ReferenceDElement REFERENCE_1015 = new ReferenceDElement("1015");
  public static final DescriptionDElement DESCRIPTION_CARTER_DE_POMPE = new DescriptionDElement("Carter de pompe");
  public static final FicheDElement FICHE_DU_CARTER = new FicheDElement(
    ELEMENT_ID_CARTER,
    Optional.of(REFERENCE_1015),
    Optional.of(DESCRIPTION_CARTER_DE_POMPE)
  );
  public static final LibelleDePoste LIBELLE_DMU_50 = new LibelleDePoste("DMU 50");
  public static final LibelleDePoste LIBELLE_TOUR_14 = new LibelleDePoste("Tour 14");
  public static final PosteConnu POSTE_CONNU_DMU_50 = new PosteConnu(POSTE_ID_DMU_50, LIBELLE_DMU_50);
  public static final PosteConnu POSTE_CONNU_TOUR_14 = new PosteConnu(POSTE_ID_TOUR, LIBELLE_TOUR_14);

  public static final OperateurId OPERATEUR_ID_DUPONT = new OperateurId(UUID.fromString("33333333-3333-3333-3333-333333333333"));
  public static final OperateurId OPERATEUR_ID_MARTIN = new OperateurId(UUID.fromString("44444444-4444-4444-4444-444444444444"));
  public static final Nom NOM_DUPONT = new Nom("Dupont");
  public static final Prenom PRENOM_JEAN = new Prenom("Jean");
  public static final OperateurConnu OPERATEUR_CONNU_DUPONT = new OperateurConnu(OPERATEUR_ID_DUPONT, NOM_DUPONT, PRENOM_JEAN);

  public static final ActiviteId ACTIVITE_ID_DU_CARTER = new ActiviteId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
  public static final ActiviteId ACTIVITE_ID_DE_LA_BRIDE = new ActiviteId(UUID.fromString("22222222-2222-2222-2222-222222222222"));

  public static final Instant LE_LUNDI_11_MAI_2026_A_MINUIT = aParis(11, 0);

  public static final Instant LE_MERCREDI_13_MAI_2026_A_8H = aParis(13, 8);

  public static final PointageId POINTAGE_ID_1 = new PointageId(UUID.fromString("00000000-0000-0000-0000-000000000001"));

  public static final PointageId POINTAGE_ID_2 = new PointageId(UUID.fromString("00000000-0000-0000-0000-000000000002"));
  public static final PointageId POINTAGE_ID_3 = new PointageId(UUID.fromString("00000000-0000-0000-0000-000000000003"));

  private SyntheseHeuresFixture() {}

  public static Activite activiteDeTravailDuCarterSurLaDmu50() {
    return Activite.builder()
      .element(ELEMENT_ID_CARTER)
      .poste(Optional.of(POSTE_ID_DMU_50))
      .nature(Optional.of(NATURE_FRAISAGE))
      .categorie(CategorieDActivite.TRAVAIL);
  }

  public static Activite activiteDeNonConformiteDuCarterSurLaDmu50() {
    return Activite.builder()
      .element(ELEMENT_ID_CARTER)
      .poste(Optional.of(POSTE_ID_DMU_50))
      .nature(Optional.of(NATURE_FRAISAGE))
      .categorie(CategorieDActivite.NON_CONFORMITE);
  }

  public static Activite activiteDeTravailDuCarterAuTour() {
    return Activite.builder()
      .element(ELEMENT_ID_CARTER)
      .poste(Optional.of(POSTE_ID_TOUR))
      .nature(Optional.of(NATURE_TOURNAGE))
      .categorie(CategorieDActivite.TRAVAIL);
  }

  public static Activite activiteDeTravailDeLaBrideSurLaDmu50() {
    return Activite.builder()
      .element(ELEMENT_ID_BRIDE)
      .poste(Optional.of(POSTE_ID_DMU_50))
      .nature(Optional.of(NATURE_FRAISAGE))
      .categorie(CategorieDActivite.TRAVAIL);
  }

  public static ActiviteInterpretee travailDuCarterDe8HA10H() {
    return ActiviteInterpretee.builder()
      .id(ACTIVITE_ID_DU_CARTER)
      .activite(activiteDeTravailDuCarterSurLaDmu50())
      .plage(new Plage(LE_LUNDI_11_MAI_2026_A_8H, Optional.of(LE_LUNDI_11_MAI_2026_A_10H)))
      .echeance(Instant.parse("2026-05-11T19:00:00Z"))
      .finAuPlusTard(Optional.empty());
  }

  public static ActiviteLue travailDuCarterLuSur(Plage plage) {
    return ActiviteLue.builder()
      .id(ACTIVITE_ID_DU_CARTER)
      .etat(plage.estOuverte() ? EtatDActivite.EN_COURS : EtatDActivite.TERMINEE)
      .plage(plage)
      .finAuPlusTard(Optional.empty());
  }

  private static Instant aParis(int jourDeMai, int heure) {
    return LocalDateTime.of(2026, 5, jourDeMai, heure, 0).atZone(ZONE_PARIS).toInstant();
  }
}
