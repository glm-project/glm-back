package com.glm.glmback.pupitre.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class PupitreFixture {

  public static final Instant LE_10_MAI_2026_A_7H = Instant.parse("2026-05-10T07:00:00Z");
  public static final Instant LE_10_MAI_2026_A_8H = Instant.parse("2026-05-10T08:00:00Z");
  public static final Instant LE_10_MAI_2026_A_9H = Instant.parse("2026-05-10T09:00:00Z");
  public static final Instant LE_10_MAI_2026_A_12H = Instant.parse("2026-05-10T12:00:00Z");

  public static final OperateurId OPERATEUR_ID_DUPONT = new OperateurId(UUID.fromString("33333333-3333-3333-3333-333333333333"));
  public static final OperateurId OPERATEUR_ID_MARTIN = new OperateurId(UUID.fromString("44444444-4444-4444-4444-444444444444"));
  public static final PosteDeTravailId POSTE_ID_FRAISEUSE_1 = new PosteDeTravailId(UUID.fromString("55555555-5555-5555-5555-555555555555"));
  public static final PosteDeTravailId POSTE_ID_FRAISEUSE_2 = new PosteDeTravailId(UUID.fromString("66666666-6666-6666-6666-666666666666"));
  public static final SuiviDuPupitreId SUIVI_ID_OF_42 = new SuiviDuPupitreId(UUID.fromString("77777777-7777-7777-7777-777777777777"));

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

  public static final OperateurDuPupitre OPERATEUR_DUPONT = OperateurDuPupitre.builder()
    .id(OPERATEUR_ID_DUPONT)
    .nom(NOM_DUPONT)
    .prenom(PRENOM_JEAN)
    .matricule(MATRICULE_049.value())
    .postes(List.of(POSTE_HABILITE_FRAISEUSE_1, POSTE_HABILITE_FRAISEUSE_2));

  private PupitreFixture() {}

  public static EvenementDuPupitre debut(Instant dateDeSurvenue) {
    return new EvenementDuPupitre(TypeDePointage.DEBUT, ACTIVITE_DUPONT_SUR_FRAISEUSE_1, dateDeSurvenue);
  }

  public static EvenementDuPupitre nonConformite(Instant dateDeSurvenue) {
    return new EvenementDuPupitre(TypeDePointage.NON_CONFORMITE, ACTIVITE_DUPONT_SUR_FRAISEUSE_1, dateDeSurvenue);
  }

  public static EvenementDuPupitre fin(Instant dateDeSurvenue) {
    return new EvenementDuPupitre(TypeDePointage.FIN, ACTIVITE_DUPONT_SUR_FRAISEUSE_1, dateDeSurvenue);
  }

  public static SuiviDuPupitre suiviOf42(JournalDuPupitre journal) {
    return SuiviDuPupitre.builder()
      .id(SUIVI_ID_OF_42)
      .nom(NOM_OF_42)
      .reference(REFERENCE_M_1187.value())
      .type(TypeDElementEngage.ORDRE_DE_FABRICATION)
      .journal(journal);
  }
}
