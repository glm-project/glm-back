package com.glm.glmback.atelier.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public final class AtelierFixture {

  public static final Instant LE_10_MAI_2026_A_7H = Instant.parse("2026-05-10T07:00:00Z");
  public static final Instant LE_10_MAI_2026_A_7H30 = Instant.parse("2026-05-10T07:30:00Z");
  public static final Instant LE_10_MAI_2026_A_8H = Instant.parse("2026-05-10T08:00:00Z");
  public static final Instant LE_10_MAI_2026_A_9H = Instant.parse("2026-05-10T09:00:00Z");
  public static final Instant LE_10_MAI_2026_A_12H = Instant.parse("2026-05-10T12:00:00Z");
  public static final Instant LE_10_MAI_2026_A_13H = Instant.parse("2026-05-10T13:00:00Z");
  public static final Instant LE_10_MAI_2026_A_16H = Instant.parse("2026-05-10T16:00:00Z");
  public static final Instant LE_10_MAI_2026_A_17H = Instant.parse("2026-05-10T17:00:00Z");
  public static final Instant LE_11_MAI_2026_A_9H15 = Instant.parse("2026-05-11T09:15:00Z");

  public static final Operateur OPERATEUR_DUPONT = new Operateur("dupont");
  public static final Operateur OPERATEUR_MARTIN = new Operateur("martin");
  public static final Auteur AUTEUR_DUPONT = new Auteur("dupont");
  public static final Auteur AUTEUR_LEROY = new Auteur("leroy");
  public static final NatureDOperation NATURE_FRAISAGE = new NatureDOperation("fraisage");
  public static final NatureDOperation NATURE_TOURNAGE = new NatureDOperation("tournage");
  public static final PosteDeTravail POSTE_FRAISEUSE_1 = new PosteDeTravail("fraiseuse 1");
  public static final PosteDeTravail POSTE_FRAISEUSE_2 = new PosteDeTravail("fraiseuse 2");
  public static final NomDElement NOM_OF_2026_000042 = new NomDElement("OF-2026-000042");
  public static final NomDElement NOM_OF_2026_000043 = new NomDElement("OF-2026-000043");
  public static final MotifDAnnulation MOTIF_ERREUR_DE_SAISIE = new MotifDAnnulation("Erreur de saisie");
  public static final ElementEngageId ELEMENT_OF_2026_000042 = new ElementEngageId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
  public static final ElementEngageId ELEMENT_OF_2026_000043 = new ElementEngageId(UUID.fromString("22222222-2222-2222-2222-222222222222"));

  private AtelierFixture() {}

  public static Periode journeeDu10Mai2026() {
    return new Periode(LE_10_MAI_2026_A_7H, LE_10_MAI_2026_A_17H);
  }

  public static ElementEngage elementEngageOf2026000042() {
    return new ElementEngage(ELEMENT_OF_2026_000042, NOM_OF_2026_000042, TypeDElementEngage.ORDRE_DE_FABRICATION);
  }

  public static ElementEngage elementEngageOf2026000043() {
    return new ElementEngage(ELEMENT_OF_2026_000043, NOM_OF_2026_000043, TypeDElementEngage.ORDRE_DE_FABRICATION);
  }

  public static Engagement engagementParLeroy() {
    return new Engagement(AUTEUR_LEROY, LE_10_MAI_2026_A_7H);
  }

  public static Cloture clotureParLeroyA(Instant date) {
    return new Cloture(AUTEUR_LEROY, Horodatage.saisiA(date));
  }

  public static SuiviDAtelier suiviDAtelierEngage() {
    return suiviDAtelierEngage(SuiviDAtelierId.newId());
  }

  public static SuiviDAtelier suiviDAtelierEngage(SuiviDAtelierId id) {
    return SuiviDAtelier.builder()
      .id(id)
      .element(elementEngageOf2026000042())
      .engagement(engagementParLeroy())
      .journal(JournalDAtelier.vide());
  }

  public static Annulation annulationParLeroy() {
    return new Annulation(AUTEUR_LEROY, LE_11_MAI_2026_A_9H15, MOTIF_ERREUR_DE_SAISIE);
  }

  public static CleDActivite cleDeFraiseuse1DeDupont() {
    return new CleDActivite(OPERATEUR_DUPONT, Optional.of(POSTE_FRAISEUSE_1));
  }

  public static EvenementDAtelier debutSurFraiseuse1ParDupontA(Instant date) {
    return pointageDeDupont(TypeDEvenementDAtelier.DEBUT, POSTE_FRAISEUSE_1, date);
  }

  public static EvenementDAtelier nonConformiteSurFraiseuse1ParDupontA(Instant date) {
    return pointageDeDupont(TypeDEvenementDAtelier.NON_CONFORMITE, POSTE_FRAISEUSE_1, date);
  }

  public static EvenementDAtelier finSurFraiseuse1ParDupontA(Instant date) {
    return pointageDeDupont(TypeDEvenementDAtelier.FIN, POSTE_FRAISEUSE_1, date);
  }

  public static EvenementDAtelier debutSurFraiseuse2ParDupontA(Instant date) {
    return pointageDeDupont(TypeDEvenementDAtelier.DEBUT, POSTE_FRAISEUSE_2, date);
  }

  public static EvenementDAtelier finSurFraiseuse2ParDupontA(Instant date) {
    return pointageDeDupont(TypeDEvenementDAtelier.FIN, POSTE_FRAISEUSE_2, date);
  }

  public static EvenementDAtelier debutSansPosteParDupontA(Instant date) {
    return evenementDAtelier(TypeDEvenementDAtelier.DEBUT, OPERATEUR_DUPONT, Optional.empty(), AUTEUR_DUPONT, Horodatage.saisiA(date));
  }

  public static EvenementDAtelier debutSurFraiseuse1ParMartinA(Instant date) {
    return evenementDAtelier(
      TypeDEvenementDAtelier.DEBUT,
      OPERATEUR_MARTIN,
      Optional.of(POSTE_FRAISEUSE_1),
      new Auteur(OPERATEUR_MARTIN.value()),
      Horodatage.saisiA(date)
    );
  }

  public static EvenementDAtelier debutSurFraiseuse1RegulariseParLeroyA(Instant date) {
    return regularisationParLeroy(TypeDEvenementDAtelier.DEBUT, date);
  }

  public static EvenementDAtelier finSurFraiseuse1RegulariseeParLeroyA(Instant date) {
    return regularisationParLeroy(TypeDEvenementDAtelier.FIN, date);
  }

  public static JourneeDeTravail journeeDeDupontOuverteA7H() {
    return JourneeDeTravail.ouverte(JourneeDeTravailId.newId(), OPERATEUR_DUPONT).enregistre(arriveeDeDupontA(LE_10_MAI_2026_A_7H));
  }

  public static JourneeDeTravail journeeDeDupontDe7HA17HAvecPauseDeMidi() {
    return journeeDeDupontOuverteA7H()
      .enregistre(pauseDeDupontA(LE_10_MAI_2026_A_12H))
      .enregistre(repriseDeDupontA(LE_10_MAI_2026_A_13H))
      .enregistre(departDeDupontA(LE_10_MAI_2026_A_17H));
  }

  public static EvenementDePresence arriveeDeDupontA(Instant date) {
    return presenceDeDupont(TypeDEvenementDePresence.ARRIVEE, date);
  }

  public static EvenementDePresence pauseDeDupontA(Instant date) {
    return presenceDeDupont(TypeDEvenementDePresence.PAUSE, date);
  }

  public static EvenementDePresence repriseDeDupontA(Instant date) {
    return presenceDeDupont(TypeDEvenementDePresence.REPRISE, date);
  }

  public static EvenementDePresence departDeDupontA(Instant date) {
    return presenceDeDupont(TypeDEvenementDePresence.DEPART, date);
  }

  public static EvenementDePresence departRegulariseParLeroyA(Instant date) {
    return EvenementDePresence.builder()
      .id(EvenementDePresenceId.newId())
      .type(TypeDEvenementDePresence.DEPART)
      .auteur(AUTEUR_LEROY)
      .horodatage(new Horodatage(date, LE_11_MAI_2026_A_9H15));
  }

  private static EvenementDePresence presenceDeDupont(TypeDEvenementDePresence type, Instant date) {
    return EvenementDePresence.builder()
      .id(EvenementDePresenceId.newId())
      .type(type)
      .auteur(AUTEUR_DUPONT)
      .horodatage(Horodatage.saisiA(date));
  }

  private static EvenementDAtelier pointageDeDupont(TypeDEvenementDAtelier type, PosteDeTravail poste, Instant date) {
    return evenementDAtelier(type, OPERATEUR_DUPONT, Optional.of(poste), AUTEUR_DUPONT, Horodatage.saisiA(date));
  }

  private static EvenementDAtelier regularisationParLeroy(TypeDEvenementDAtelier type, Instant date) {
    return evenementDAtelier(
      type,
      OPERATEUR_DUPONT,
      Optional.of(POSTE_FRAISEUSE_1),
      AUTEUR_LEROY,
      new Horodatage(date, LE_11_MAI_2026_A_9H15)
    );
  }

  private static EvenementDAtelier evenementDAtelier(
    TypeDEvenementDAtelier type,
    Operateur operateur,
    Optional<PosteDeTravail> poste,
    Auteur auteur,
    Horodatage horodatage
  ) {
    return EvenementDAtelier.builder()
      .id(EvenementDAtelierId.newId())
      .type(type)
      .operateur(operateur)
      .poste(poste)
      .nature(Optional.of(NATURE_FRAISAGE))
      .auteur(auteur)
      .horodatage(horodatage);
  }
}
