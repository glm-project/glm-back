package com.glm.glmback.atelier.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class AtelierFixture {

  public static final CategorieDElement CATEGORIE_OF = new CategorieDElement("OF");
  public static final CategorieDElement CATEGORIE_MOULE = new CategorieDElement("MOULE");

  public static final Instant LE_10_MAI_2026_A_7H = Instant.parse("2026-05-10T07:00:00Z");
  public static final Instant LE_10_MAI_2026_A_7H30 = Instant.parse("2026-05-10T07:30:00Z");
  public static final Instant LE_10_MAI_2026_A_8H = Instant.parse("2026-05-10T08:00:00Z");
  public static final Instant LE_10_MAI_2026_A_9H = Instant.parse("2026-05-10T09:00:00Z");
  public static final Instant LE_10_MAI_2026_A_12H = Instant.parse("2026-05-10T12:00:00Z");
  public static final Instant LE_10_MAI_2026_A_13H = Instant.parse("2026-05-10T13:00:00Z");
  public static final Instant LE_10_MAI_2026_A_16H = Instant.parse("2026-05-10T16:00:00Z");
  public static final Instant LE_10_MAI_2026_A_17H = Instant.parse("2026-05-10T17:00:00Z");
  public static final Instant LE_11_MAI_2026_A_9H15 = Instant.parse("2026-05-11T09:15:00Z");
  public static final Instant LE_10_MAI_2026_A_20H = Instant.parse("2026-05-10T20:00:00Z");
  public static final Instant LE_11_MAI_2026_A_3H = Instant.parse("2026-05-11T03:00:00Z");
  public static final Instant LE_11_MAI_2026_A_7H = Instant.parse("2026-05-11T07:00:00Z");
  public static final Instant LE_11_MAI_2026_A_8H = Instant.parse("2026-05-11T08:00:00Z");
  public static final Instant LE_11_MAI_2026_A_8H30 = Instant.parse("2026-05-11T08:30:00Z");
  public static final Instant LE_11_MAI_2026_A_9H = Instant.parse("2026-05-11T09:00:00Z");
  public static final Instant LE_11_MAI_2026_A_20H = Instant.parse("2026-05-11T20:00:00Z");

  public static final OperateurId OPERATEUR_ID_DUPONT = new OperateurId(UUID.fromString("33333333-3333-3333-3333-333333333333"));
  public static final OperateurId OPERATEUR_ID_MARTIN = new OperateurId(UUID.fromString("44444444-4444-4444-4444-444444444444"));
  public static final PosteDeTravailId POSTE_ID_FRAISEUSE_1 = new PosteDeTravailId(UUID.fromString("55555555-5555-5555-5555-555555555555"));
  public static final PosteDeTravailId POSTE_ID_FRAISEUSE_2 = new PosteDeTravailId(UUID.fromString("66666666-6666-6666-6666-666666666666"));
  public static final Nom NOM_DUPONT = new Nom("Dupont");
  public static final Nom NOM_MARTIN = new Nom("Martin");
  public static final Prenom PRENOM_JEAN = new Prenom("Jean");
  public static final Prenom PRENOM_PAUL = new Prenom("Paul");
  public static final LibelleDePoste LIBELLE_FRAISEUSE_1 = new LibelleDePoste("Fraiseuse 1");
  public static final LibelleDePoste LIBELLE_FRAISEUSE_2 = new LibelleDePoste("Fraiseuse 2");
  public static final Auteur AUTEUR_DUPONT = new Auteur("dupont");
  public static final Auteur AUTEUR_MARTIN = new Auteur("martin");
  public static final Auteur AUTEUR_LEROY = new Auteur("leroy");
  public static final NatureDOperation NATURE_FRAISAGE = new NatureDOperation("fraisage");
  public static final NatureDOperation NATURE_TOURNAGE = new NatureDOperation("tournage");
  public static final CoutHoraire COUT_HORAIRE_FRAISEUSE_1 = new CoutHoraire(new BigDecimal("45.50"));
  public static final TauxHoraire TAUX_HORAIRE_DUPONT = new TauxHoraire(new BigDecimal("22.00"));
  public static final OperateurConnu OPERATEUR_CONNU_DUPONT = OperateurConnu.builder()
    .id(OPERATEUR_ID_DUPONT)
    .nom(NOM_DUPONT)
    .prenom(PRENOM_JEAN)
    .tauxHoraire(TAUX_HORAIRE_DUPONT.value());
  public static final OperateurConnu OPERATEUR_CONNU_MARTIN = OperateurConnu.builder()
    .id(OPERATEUR_ID_MARTIN)
    .nom(NOM_MARTIN)
    .prenom(PRENOM_PAUL)
    .tauxHoraire(null);
  public static final PosteConnu POSTE_CONNU_FRAISEUSE_1 = PosteConnu.builder()
    .id(POSTE_ID_FRAISEUSE_1)
    .libelle(LIBELLE_FRAISEUSE_1)
    .nature(NATURE_FRAISAGE)
    .coutHoraire(COUT_HORAIRE_FRAISEUSE_1.value());
  public static final PosteConnu POSTE_CONNU_FRAISEUSE_2 = PosteConnu.builder()
    .id(POSTE_ID_FRAISEUSE_2)
    .libelle(LIBELLE_FRAISEUSE_2)
    .nature(NATURE_TOURNAGE)
    .coutHoraire(null);
  public static final NomDElement NOM_OF_2026_000042 = new NomDElement("OF-2026-000042");
  public static final NomDElement NOM_OF_2026_000043 = new NomDElement("OF-2026-000043");
  public static final ElementEngageId ELEMENT_OF_2026_000042 = new ElementEngageId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
  public static final ElementEngageId ELEMENT_OF_2026_000043 = new ElementEngageId(UUID.fromString("22222222-2222-2222-2222-222222222222"));

  private AtelierFixture() {}

  /**
   * Un geste d'atelier dont tout est connu, sauf l'heure a laquelle il survient.
   */
  @FunctionalInterface
  public interface GesteADater {
    EvenementDAtelier a(Instant date);
  }

  public static Periode journeeDu10Mai2026() {
    return new Periode(LE_10_MAI_2026_A_7H, LE_10_MAI_2026_A_17H);
  }

  public static ElementEngage elementEngageOf2026000042() {
    return new ElementEngage(ELEMENT_OF_2026_000042, NOM_OF_2026_000042, CATEGORIE_OF);
  }

  public static ElementEngage elementEngageOf2026000043() {
    return new ElementEngage(ELEMENT_OF_2026_000043, NOM_OF_2026_000043, CATEGORIE_OF);
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

  public static AnnuaireDAtelier annuaireDeDupontEtMartin() {
    return AnnuaireDAtelier.de(
      List.of(OPERATEUR_CONNU_DUPONT, OPERATEUR_CONNU_MARTIN),
      List.of(POSTE_CONNU_FRAISEUSE_1, POSTE_CONNU_FRAISEUSE_2)
    );
  }

  public static List<SuiviDAtelier> suivisDAtelierEnAttenteEnCoursInterrompuEtCloture() {
    EvenementDAtelier debut = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    SuiviDAtelier enCours = suiviDAtelierEngage().enregistre(debut);
    return List.of(
      suiviDAtelierEngage(),
      enCours,
      enCours.enregistre(finDe(debut).a(LE_10_MAI_2026_A_9H)).enregistre(nonConformiteSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_9H)),
      enCours.enregistre(finDe(debut).a(LE_10_MAI_2026_A_9H)),
      enCours.cloture(clotureParLeroyA(LE_10_MAI_2026_A_9H)),
      suiviDAtelierEngage().enregistre(debutSansPosteParDupontA(LE_10_MAI_2026_A_8H))
    );
  }

  public static SuiviDAtelier suiviDAtelierAvecCentEvenements() {
    SuiviDAtelier suivi = suiviDAtelierEngage();
    for (int jour = 0; jour < 24; jour++) {
      Instant debut = LE_10_MAI_2026_A_8H.plusSeconds(jour * 86400L);
      EvenementDAtelier debutDeDupont = debutSurFraiseuse1ParDupontA(debut);
      EvenementDAtelier debutDeMartin = debutSurFraiseuse1ParMartinA(debut.plusSeconds(60));
      suivi = suivi
        .enregistre(debutDeDupont)
        .enregistre(debutDeMartin)
        .enregistre(finDe(debutDeDupont).a(debut.plusSeconds(28800)))
        .enregistre(finDe(debutDeMartin).a(debut.plusSeconds(28860)));
    }
    Instant debut = LE_10_MAI_2026_A_8H.plusSeconds(24 * 86400L);
    EvenementDAtelier debutDeDupont = debutSurFraiseuse1ParDupontA(debut);
    EvenementDAtelier debutDeMartin = debutSurFraiseuse1ParMartinA(debut.plusSeconds(60));
    return suivi
      .enregistre(debutDeDupont)
      .enregistre(debutDeMartin)
      .enregistre(finDe(debutDeDupont).a(debut.plusSeconds(120)))
      .enregistre(nonConformiteSurFraiseuse1ParDupontA(debut.plusSeconds(120)));
  }

  public static CleDActivite cleDeFraiseuse1DeDupont() {
    return new CleDActivite(OPERATEUR_ID_DUPONT, Optional.of(POSTE_ID_FRAISEUSE_1));
  }

  public static EvenementDAtelier debutSurFraiseuse1ParDupontA(Instant date) {
    return ouvertureDeDupont(TypeDEvenementDAtelier.DEBUT, POSTE_ID_FRAISEUSE_1, date);
  }

  /**
   * Une non conformite ouverte d'emblee : celle qu'on pointe apres la fin d'une activite, ou a la reprise d'une pause.
   */
  public static EvenementDAtelier nonConformiteSurFraiseuse1ParDupontA(Instant date) {
    return ouvertureDeDupont(TypeDEvenementDAtelier.NON_CONFORMITE, POSTE_ID_FRAISEUSE_1, date);
  }

  public static EvenementDAtelier debutSurFraiseuse2ParDupontA(Instant date) {
    return ouvertureDeDupont(TypeDEvenementDAtelier.DEBUT, POSTE_ID_FRAISEUSE_2, date);
  }

  public static EvenementDAtelier debutSansPosteParDupontA(Instant date) {
    return pointageDAtelier(
      TypeDEvenementDAtelier.DEBUT,
      Optional.empty(),
      new CleDActivite(OPERATEUR_ID_DUPONT, Optional.empty()),
      AUTEUR_DUPONT,
      Horodatage.saisiA(date)
    );
  }

  public static EvenementDAtelier debutSansNatureSurFraiseuse1ParDupontA(Instant date) {
    EvenementDAtelierId id = EvenementDAtelierId.newId();
    return EvenementDAtelier.builder()
      .id(id)
      .type(TypeDEvenementDAtelier.DEBUT)
      .activite(Optional.of(ActiviteId.ouvertePar(id)))
      .activiteVisee(Optional.empty())
      .operateur(OPERATEUR_ID_DUPONT)
      .poste(Optional.of(POSTE_ID_FRAISEUSE_1))
      .nature(Optional.empty())
      .coutHoraire(Optional.empty())
      .tauxHoraire(Optional.empty())
      .auteur(AUTEUR_DUPONT)
      .origine(OrigineDuPointage.POINTAGE)
      .horodatage(Horodatage.saisiA(date));
  }

  public static EvenementDAtelier debutSurFraiseuse1ParMartinA(Instant date) {
    return pointageDAtelier(
      TypeDEvenementDAtelier.DEBUT,
      Optional.empty(),
      new CleDActivite(OPERATEUR_ID_MARTIN, Optional.of(POSTE_ID_FRAISEUSE_1)),
      AUTEUR_MARTIN,
      Horodatage.saisiA(date)
    );
  }

  /**
   * La fin pointee sur la cle de l'activite qu'ouvre ce debut, par celui qui l'a ouverte : elle ferme l'activite en
   * cours de la cle sans la designer, et il ne reste qu'a la dater.
   */
  public static GesteADater finDe(EvenementDAtelier ouvrant) {
    return date -> pointageDAtelier(TypeDEvenementDAtelier.FIN, Optional.empty(), ouvrant.cle(), ouvrant.auteur(), Horodatage.saisiA(date));
  }

  /**
   * La fin que le gestionnaire regularise sur l'activite de Dupont qu'ouvre ce debut : la seule fin qui porte une cible.
   */
  public static GesteADater finRegulariseeParLeroyDe(EvenementDAtelier ouvrant) {
    return date ->
      evenementDAtelier(
        TypeDEvenementDAtelier.FIN,
        ouvrant.activite(),
        cleDeFraiseuse1DeDupont(),
        AUTEUR_LEROY,
        OrigineDuPointage.REGULARISATION,
        new Horodatage(date, LE_11_MAI_2026_A_9H15)
      );
  }

  private static EvenementDAtelier ouvertureDeDupont(TypeDEvenementDAtelier type, PosteDeTravailId poste, Instant date) {
    return pointageDAtelier(
      type,
      Optional.empty(),
      new CleDActivite(OPERATEUR_ID_DUPONT, Optional.of(poste)),
      AUTEUR_DUPONT,
      Horodatage.saisiA(date)
    );
  }

  private static EvenementDAtelier pointageDAtelier(
    TypeDEvenementDAtelier type,
    Optional<ActiviteId> activiteVisee,
    CleDActivite cle,
    Auteur auteur,
    Horodatage horodatage
  ) {
    return evenementDAtelier(type, activiteVisee, cle, auteur, OrigineDuPointage.POINTAGE, horodatage);
  }

  private static EvenementDAtelier evenementDAtelier(
    TypeDEvenementDAtelier type,
    Optional<ActiviteId> activiteVisee,
    CleDActivite cle,
    Auteur auteur,
    OrigineDuPointage origine,
    Horodatage horodatage
  ) {
    EvenementDAtelierId id = EvenementDAtelierId.newId();

    return EvenementDAtelier.builder()
      .id(id)
      .type(type)
      .activite(type == TypeDEvenementDAtelier.FIN ? Optional.empty() : Optional.of(ActiviteId.ouvertePar(id)))
      .activiteVisee(activiteVisee)
      .operateur(cle.operateur())
      .poste(cle.poste())
      .nature(Optional.of(NATURE_FRAISAGE))
      .coutHoraire(Optional.of(COUT_HORAIRE_FRAISEUSE_1))
      .tauxHoraire(Optional.of(TAUX_HORAIRE_DUPONT))
      .auteur(auteur)
      .origine(origine)
      .horodatage(horodatage);
  }
}
