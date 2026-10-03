package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class LectureDossierConflitTest {

  @Test
  void shouldLireLaSequenceEtLeDiagnosticDepuisUnPointageActif() {
    EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier nonConformite = passageEnNonConformiteDe(travail).a(LE_10_MAI_2026_A_12H);
    EvenementDAtelier finDuTravail = finDe(travail).a(LE_10_MAI_2026_A_17H);
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(travail).enregistre(nonConformite).enregistre(finDuTravail);

    LectureDossierConflit dossier = new LectureDossierConflit(
      new AdresseDossierConflit(suivi.id(), finDuTravail.id()),
      new LectureDuSuivi(suivi, LE_10_MAI_2026_A_17H)
    );

    assertThat(dossier.sequence()).contains(suivi.conflits().getFirst());
    assertThat(dossier.diagnostics()).singleElement().extracting(DiagnosticDeConflit::pointage).isEqualTo(finDuTravail.id());
    assertThat(dossier.lecture().suivi().journal().evenements()).containsExactly(travail, nonConformite, finDuTravail);
    assertThat(dossier.lecture().suivi().revision()).isEqualTo(suivi.revision());
  }

  @Test
  void shouldRendreUneAdresseIntrouvableAvecLeJournalAccessible() {
    EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier autrePointage = finDe(travail).a(LE_10_MAI_2026_A_17H);
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(travail);

    LectureDossierConflit dossier = new LectureDossierConflit(
      new AdresseDossierConflit(suivi.id(), autrePointage.id()),
      new LectureDuSuivi(suivi, LE_10_MAI_2026_A_17H)
    );

    assertThat(dossier.kind()).isEqualTo(EtatDAdresseDossier.INTROUVABLE);
    assertThat(dossier.sequence()).isEmpty();
    assertThat(dossier.diagnostics()).isEmpty();
    assertThat(dossier.lecture().suivi().journal().evenements()).containsExactly(travail);
  }

  @Test
  void shouldSignalerUneAncreAnnuleeSansOuvrirUneAutreSequence() {
    EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    SuiviDAtelier suivi = suiviDAtelierEngage()
      .enregistre(travail)
      .annule(travail.id(), new Annulation(AUTEUR_LEROY, LE_10_MAI_2026_A_17H, MOTIF_ERREUR_DE_SAISIE));

    LectureDossierConflit dossier = new LectureDossierConflit(
      new AdresseDossierConflit(suivi.id(), travail.id()),
      new LectureDuSuivi(suivi, LE_10_MAI_2026_A_17H)
    );

    assertThat(dossier.kind().name()).isEqualTo("ANCRE_ANNULEE");
    assertThat(dossier.sequence()).isEmpty();
    assertThat(dossier.lecture().suivi().journal().evenement(travail.id())).get().extracting(EvenementDAtelier::estAnnule).isEqualTo(true);
  }
}
