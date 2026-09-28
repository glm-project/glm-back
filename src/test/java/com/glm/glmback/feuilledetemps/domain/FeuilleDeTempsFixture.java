package com.glm.glmback.feuilledetemps.domain;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * La semaine 20 de 2026, du lundi 11 au dimanche 17 mai, vue de Paris en heure d'ete.
 */
public final class FeuilleDeTempsFixture {

  public static final ZoneId ZONE_PARIS = ZoneId.of("Europe/Paris");
  public static final SemaineCalendaire SEMAINE_20_DE_2026 = new SemaineCalendaire(2026, 20);

  public static final LocalDate LUNDI_11_MAI_2026 = LocalDate.of(2026, 5, 11);
  public static final LocalDate MARDI_12_MAI_2026 = LocalDate.of(2026, 5, 12);
  public static final LocalDate MERCREDI_13_MAI_2026 = LocalDate.of(2026, 5, 13);

  public static final SemaineCalendaire SEMAINE_19_DE_2026 = new SemaineCalendaire(2026, 19);
  public static final LocalDate DIMANCHE_10_MAI_2026 = LocalDate.of(2026, 5, 10);
  public static final AmplitudeMaximale AMPLITUDE_MAXIMALE_13H = new AmplitudeMaximale(Duration.ofHours(13));
  public static final Instant LE_DIMANCHE_10_MAI_2026_A_20H = aParis(10, 20);
  public static final Instant LE_LUNDI_11_MAI_2026_A_MINUIT = aParis(11, 0);
  public static final Instant LE_LUNDI_11_MAI_2026_A_7H = aParis(11, 7);
  public static final Instant LE_LUNDI_11_MAI_2026_A_16H = aParis(11, 16);
  public static final Instant LE_LUNDI_11_MAI_2026_A_20H = aParis(11, 20);
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
  public static final Instant LE_MERCREDI_13_MAI_2026_A_8H = aParis(13, 8);
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

  public static final OperateurId OPERATEUR_ID_DUPONT = new OperateurId(UUID.fromString("33333333-3333-3333-3333-333333333333"));
  public static final OperateurId OPERATEUR_ID_MARTIN = new OperateurId(UUID.fromString("44444444-4444-4444-4444-444444444444"));
  public static final Nom NOM_DUPONT = new Nom("Dupont");
  public static final Prenom PRENOM_JEAN = new Prenom("Jean");
  public static final OperateurConnu OPERATEUR_CONNU_DUPONT = new OperateurConnu(OPERATEUR_ID_DUPONT, NOM_DUPONT, PRENOM_JEAN);

  private FeuilleDeTempsFixture() {}

  public static EvenementDePresence arriveeA(Instant date) {
    return new EvenementDePresence(TypeDEvenementDePresence.ARRIVEE, date);
  }

  public static EvenementDePresence departA(Instant date) {
    return new EvenementDePresence(TypeDEvenementDePresence.DEPART, date);
  }

  public static JourneeDeTravail journeeDuLundiDe8HA17H() {
    return new JourneeDeTravail(List.of(arriveeA(LE_LUNDI_11_MAI_2026_A_8H), departA(LE_LUNDI_11_MAI_2026_A_17H)));
  }

  public static JourneeDeTravail journeeDuLundi22HAuMardi2H() {
    return new JourneeDeTravail(List.of(arriveeA(LE_LUNDI_11_MAI_2026_A_22H), departA(LE_MARDI_12_MAI_2026_A_2H)));
  }

  public static JourneeDeTravail journeeDuDimanchePrecedentDe8HA17H() {
    return new JourneeDeTravail(List.of(arriveeA(LE_DIMANCHE_10_MAI_2026_A_8H), departA(LE_DIMANCHE_10_MAI_2026_A_17H)));
  }

  public static JourneeDeTravail journeeDuMardiOuverteA8H() {
    return new JourneeDeTravail(List.of(arriveeA(LE_MARDI_12_MAI_2026_A_8H)));
  }

  /**
   * E2 : lundi, Dupont arrive a 7 h et part sans pointer son depart.
   */
  public static JourneeDeTravail journeeDuLundiDe7HSansDepart() {
    return new JourneeDeTravail(List.of(arriveeA(LE_LUNDI_11_MAI_2026_A_7H)));
  }

  /**
   * Issue #59 : lundi, Dupont arrive a 7 h, puis ne pointe plus rien avant un depart mercredi a 8 h, rattache a la
   * meme journee par une version anterieure au lot 3.
   */
  public static JourneeDeTravail journeeDuLundi7HAuMercredi8H() {
    return new JourneeDeTravail(List.of(arriveeA(LE_LUNDI_11_MAI_2026_A_7H), departA(LE_MERCREDI_13_MAI_2026_A_8H)));
  }

  /**
   * E7 : un poste de nuit du dimanche 20 h au lundi 8 h, a cheval sur deux semaines.
   */
  public static JourneeDeTravail journeeDuDimanche20HAuLundi8H() {
    return new JourneeDeTravail(List.of(arriveeA(LE_DIMANCHE_10_MAI_2026_A_20H), departA(LE_LUNDI_11_MAI_2026_A_8H)));
  }

  public static PointageDAtelier debutSurLaDmu50A(Instant date) {
    return surLaDmu50(TypeDEvenementDAtelier.DEBUT, date);
  }

  public static PointageDAtelier nonConformiteSurLaDmu50A(Instant date) {
    return surLaDmu50(TypeDEvenementDAtelier.NON_CONFORMITE, date);
  }

  public static PointageDAtelier finSurLaDmu50A(Instant date) {
    return surLaDmu50(TypeDEvenementDAtelier.FIN, date);
  }

  public static PointageDAtelier debutAuTourA(Instant date) {
    return auTour(TypeDEvenementDAtelier.DEBUT, date);
  }

  public static PointageDAtelier finAuTourA(Instant date) {
    return auTour(TypeDEvenementDAtelier.FIN, date);
  }

  public static PointageDAtelier debutSansPosteA(Instant date) {
    return PointageDAtelier.builder()
      .type(TypeDEvenementDAtelier.DEBUT)
      .poste(Optional.empty())
      .nature(Optional.empty())
      .dateDeSurvenue(date);
  }

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

  private static PointageDAtelier surLaDmu50(TypeDEvenementDAtelier type, Instant date) {
    return PointageDAtelier.builder()
      .type(type)
      .poste(Optional.of(POSTE_ID_DMU_50))
      .nature(Optional.of(NATURE_FRAISAGE))
      .dateDeSurvenue(date);
  }

  private static PointageDAtelier auTour(TypeDEvenementDAtelier type, Instant date) {
    return PointageDAtelier.builder()
      .type(type)
      .poste(Optional.of(POSTE_ID_TOUR))
      .nature(Optional.of(NATURE_TOURNAGE))
      .dateDeSurvenue(date);
  }

  private static Instant aParis(int jourDeMai, int heure) {
    return LocalDateTime.of(2026, 5, jourDeMai, heure, 0).atZone(ZONE_PARIS).toInstant();
  }
}
