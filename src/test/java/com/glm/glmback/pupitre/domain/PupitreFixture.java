package com.glm.glmback.pupitre.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class PupitreFixture {

  public static final Instant LE_10_MAI_2026_A_7H = Instant.parse("2026-05-10T07:00:00Z");
  public static final Instant LE_10_MAI_2026_A_8H = Instant.parse("2026-05-10T08:00:00Z");
  public static final Instant LE_10_MAI_2026_A_9H = Instant.parse("2026-05-10T09:00:00Z");
  public static final Instant LE_10_MAI_2026_A_12H = Instant.parse("2026-05-10T12:00:00Z");
  public static final Instant LE_10_MAI_2026_A_20H = Instant.parse("2026-05-10T20:00:00Z");
  public static final Instant LE_10_MAI_2026_A_21H = Instant.parse("2026-05-10T21:00:00Z");
  public static final AmplitudeMaximale AMPLITUDE_MAXIMALE_13H = new AmplitudeMaximale(Duration.ofHours(13));
  public static final PresenceDuPupitre PRESENCE_PRESENTE_JUSQU_A_20H = new PresenceDuPupitre(
    EtatDePresence.PRESENT,
    Optional.of(Instant.parse("2026-05-10T20:00:00Z"))
  );

  public static final OperateurId OPERATEUR_ID_DUPONT = new OperateurId(UUID.fromString("33333333-3333-3333-3333-333333333333"));
  public static final OperateurId OPERATEUR_ID_MARTIN = new OperateurId(UUID.fromString("44444444-4444-4444-4444-444444444444"));
  public static final PosteDeTravailId POSTE_ID_FRAISEUSE_1 = new PosteDeTravailId(UUID.fromString("55555555-5555-5555-5555-555555555555"));
  public static final PosteDeTravailId POSTE_ID_FRAISEUSE_2 = new PosteDeTravailId(UUID.fromString("66666666-6666-6666-6666-666666666666"));
  public static final SuiviDuPupitreId SUIVI_ID_OF_42 = new SuiviDuPupitreId(UUID.fromString("77777777-7777-7777-7777-777777777777"));
  public static final ActiviteId ACTIVITE_ID_88888888 = new ActiviteId(UUID.fromString("88888888-8888-8888-8888-888888888888"));

  public static final Nom NOM_DUPONT = new Nom("Dupont");
  public static final Prenom PRENOM_JEAN = new Prenom("Jean");
  public static final Matricule MATRICULE_049 = new Matricule("049");
  public static final LibelleDePoste LIBELLE_FRAISEUSE_1 = new LibelleDePoste("Fraiseuse 1");
  public static final LibelleDePoste LIBELLE_FRAISEUSE_2 = new LibelleDePoste("Fraiseuse 2");
  public static final NomDElement NOM_OF_42 = new NomDElement("OF-2026-000042");
  public static final ReferenceDElement REFERENCE_M_1187 = new ReferenceDElement("M-1187");

  public static final CleDActivite ACTIVITE_DUPONT_SUR_FRAISEUSE_1 = new CleDActivite(
    OPERATEUR_ID_DUPONT,
    Optional.of(POSTE_ID_FRAISEUSE_1)
  );
  public static final CleDActivite ACTIVITE_DUPONT_SANS_POSTE = new CleDActivite(OPERATEUR_ID_DUPONT, Optional.empty());
  public static final CleDActivite ACTIVITE_MARTIN_SUR_FRAISEUSE_2 = new CleDActivite(
    OPERATEUR_ID_MARTIN,
    Optional.of(POSTE_ID_FRAISEUSE_2)
  );

  public static final PosteHabilite POSTE_HABILITE_FRAISEUSE_1 = new PosteHabilite(POSTE_ID_FRAISEUSE_1, LIBELLE_FRAISEUSE_1);
  public static final PosteHabilite POSTE_HABILITE_FRAISEUSE_2 = new PosteHabilite(POSTE_ID_FRAISEUSE_2, LIBELLE_FRAISEUSE_2);

  public static final OperateurDuPupitre OPERATEUR_DUPONT = operateurDupont(PRESENCE_PRESENTE_JUSQU_A_20H);

  private PupitreFixture() {}

  public static OperateurDuPupitre operateurDupont(PresenceDuPupitre presence) {
    return OperateurDuPupitre.builder()
      .id(OPERATEUR_ID_DUPONT)
      .nom(NOM_DUPONT)
      .prenom(PRENOM_JEAN)
      .matricule(MATRICULE_049.value())
      .etat(presence.etat())
      .presentJusqua(presence.presentJusqua())
      .postes(List.of(POSTE_HABILITE_FRAISEUSE_1, POSTE_HABILITE_FRAISEUSE_2));
  }

  /**
   * Le travail de Dupont sur la fraiseuse 1, ouvert a 8 h et que rien n'a termine : son echeance tombe a 21 h.
   */
  public static ActiviteSansFin travailDeDupontSurFraiseuse1Depuis8H() {
    return ActiviteSansFin.builder()
      .ouverture(ACTIVITE_ID_88888888)
      .activite(ACTIVITE_DUPONT_SUR_FRAISEUSE_1)
      .categorie(CategorieDActivite.TRAVAIL)
      .depuis(LE_10_MAI_2026_A_8H)
      .echeance(LE_10_MAI_2026_A_21H);
  }

  /**
   * L'OF 42 engage, sur lequel personne n'a encore pointe.
   */
  public static SuiviDuPupitre suiviOf42Vierge() {
    return SuiviDuPupitre.builder()
      .id(SUIVI_ID_OF_42)
      .nom(NOM_OF_42)
      .reference(REFERENCE_M_1187.value())
      .type(TypeDElementEngage.ORDRE_DE_FABRICATION)
      .activites(List.of())
      .dejaPointe(false);
  }

  /**
   * L'OF 42 deja pointe, avec ses activites que rien n'a terminees.
   */
  public static SuiviDuPupitre suiviOf42Pointe(List<ActiviteSansFin> activites) {
    return SuiviDuPupitre.builder()
      .id(SUIVI_ID_OF_42)
      .nom(NOM_OF_42)
      .reference(REFERENCE_M_1187.value())
      .type(TypeDElementEngage.ORDRE_DE_FABRICATION)
      .activites(activites)
      .dejaPointe(true);
  }
}
