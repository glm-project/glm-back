package com.glm.glmback.feuilledetemps.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
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

  public static final Instant LE_DIMANCHE_10_MAI_2026_A_8H = aParis(10, 8);
  public static final Instant LE_DIMANCHE_10_MAI_2026_A_17H = aParis(10, 17);
  public static final Instant LE_LUNDI_11_MAI_2026_A_8H = aParis(11, 8);
  public static final Instant LE_LUNDI_11_MAI_2026_A_12H = aParis(11, 12);
  public static final Instant LE_LUNDI_11_MAI_2026_A_13H = aParis(11, 13);
  public static final Instant LE_LUNDI_11_MAI_2026_A_17H = aParis(11, 17);
  public static final Instant LE_LUNDI_11_MAI_2026_A_22H = aParis(11, 22);
  public static final Instant LE_MARDI_12_MAI_2026_A_MINUIT = aParis(12, 0);
  public static final Instant LE_MARDI_12_MAI_2026_A_2H = aParis(12, 2);
  public static final Instant LE_MARDI_12_MAI_2026_A_8H = aParis(12, 8);
  public static final Instant LE_MERCREDI_13_MAI_2026_A_8H = aParis(13, 8);

  public static final OperateurId OPERATEUR_ID_DUPONT = new OperateurId(UUID.fromString("33333333-3333-3333-3333-333333333333"));
  public static final OperateurId OPERATEUR_ID_MARTIN = new OperateurId(UUID.fromString("44444444-4444-4444-4444-444444444444"));
  public static final Nom NOM_DUPONT = new Nom("Dupont");
  public static final Prenom PRENOM_JEAN = new Prenom("Jean");
  public static final OperateurConnu OPERATEUR_CONNU_DUPONT = new OperateurConnu(OPERATEUR_ID_DUPONT, NOM_DUPONT, PRENOM_JEAN);

  private FeuilleDeTempsFixture() {}

  public static EvenementDePresence arriveeA(Instant date) {
    return new EvenementDePresence(TypeDEvenementDePresence.ARRIVEE, date);
  }

  public static EvenementDePresence pauseA(Instant date) {
    return new EvenementDePresence(TypeDEvenementDePresence.PAUSE, date);
  }

  public static EvenementDePresence repriseA(Instant date) {
    return new EvenementDePresence(TypeDEvenementDePresence.REPRISE, date);
  }

  public static EvenementDePresence departA(Instant date) {
    return new EvenementDePresence(TypeDEvenementDePresence.DEPART, date);
  }

  public static JourneeDeTravail journeeDuLundiDe8HA17HAvecPauseDeMidi() {
    return new JourneeDeTravail(
      List.of(
        arriveeA(LE_LUNDI_11_MAI_2026_A_8H),
        pauseA(LE_LUNDI_11_MAI_2026_A_12H),
        repriseA(LE_LUNDI_11_MAI_2026_A_13H),
        departA(LE_LUNDI_11_MAI_2026_A_17H)
      )
    );
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

  private static Instant aParis(int jourDeMai, int heure) {
    return LocalDateTime.of(2026, 5, jourDeMai, heure, 0).atZone(ZONE_PARIS).toInstant();
  }
}
