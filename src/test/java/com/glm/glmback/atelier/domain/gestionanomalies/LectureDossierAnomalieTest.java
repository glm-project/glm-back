package com.glm.glmback.atelier.domain.gestionanomalies;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.atelier.domain.Annulation;
import com.glm.glmback.atelier.domain.DiagnosticDeConflit;
import com.glm.glmback.atelier.domain.EvenementDAtelier;
import com.glm.glmback.atelier.domain.LectureDuSuivi;
import com.glm.glmback.atelier.domain.SuiviDAtelier;
import org.junit.jupiter.api.Test;

@UnitTest
class LectureDossierAnomalieTest {

  @Test
  void shouldLireLaSequenceEtLeDiagnosticDepuisUnPointageActif() {
    EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier nonConformite = passageEnNonConformiteDe(travail).a(LE_10_MAI_2026_A_12H);
    EvenementDAtelier finDuTravail = finDe(travail).a(LE_10_MAI_2026_A_17H);
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(travail).enregistre(nonConformite).enregistre(finDuTravail);

    LectureDossierAnomalie dossier = new LectureDossierAnomalie(
      new AdresseDossierAnomalie(suivi.id(), finDuTravail.id()),
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

    LectureDossierAnomalie dossier = new LectureDossierAnomalie(
      new AdresseDossierAnomalie(suivi.id(), autrePointage.id()),
      new LectureDuSuivi(suivi, LE_10_MAI_2026_A_17H)
    );

    assertThat(dossier.kind()).isEqualTo(EtatDAdresseDossier.INTROUVABLE);
    assertThat(dossier.enConflit()).isFalse();
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

    LectureDossierAnomalie dossier = new LectureDossierAnomalie(
      new AdresseDossierAnomalie(suivi.id(), travail.id()),
      new LectureDuSuivi(suivi, LE_10_MAI_2026_A_17H)
    );

    assertThat(dossier.kind().name()).isEqualTo("ANCRE_ANNULEE");
    assertThat(dossier.sequence()).isEmpty();
    assertThat(dossier.lecture().suivi().journal().evenement(travail.id())).get().extracting(EvenementDAtelier::estAnnule).isEqualTo(true);
  }

  @Test
  void shouldRendreSansAnomalieUnPointageActifSansConflitNiFinAutomatique() {
    EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(travail);

    LectureDossierAnomalie dossier = new LectureDossierAnomalie(
      new AdresseDossierAnomalie(suivi.id(), travail.id()),
      new LectureDuSuivi(suivi, LE_10_MAI_2026_A_17H)
    );

    assertThat(dossier.kind().name()).isEqualTo("SANS_ANOMALIE");
    assertThat(dossier.sequence()).isEmpty();
    assertThat(dossier.lecture().suivi().journal().evenements()).containsExactly(travail);
  }
}
