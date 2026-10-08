package com.glm.glmback.atelier.domain.gestionanomalies;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.atelier.domain.EvenementDAtelier;
import com.glm.glmback.atelier.domain.LectureDuSuivi;
import com.glm.glmback.atelier.domain.SuiviDAtelier;
import java.time.Instant;
import org.junit.jupiter.api.Test;

@UnitTest
class LectureDossierAnomalieTest {

  private static final Instant LE_10_MAI_2026_A_21H = Instant.parse("2026-05-10T21:00:00Z");
  private static final Instant LE_10_MAI_2026_A_22H = Instant.parse("2026-05-10T22:00:00Z");
  private static final Instant LE_10_MAI_2026_A_23H = Instant.parse("2026-05-10T23:00:00Z");

  @Test
  void shouldOuvrirLeDossierExactementALEcheance() {
    var travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var suivi = suiviDAtelierEngage().enregistre(travail);

    assertThat(dossier(suivi, travail, LE_10_MAI_2026_A_21H).activite().finAutomatique()).isTrue();
    assertThatThrownBy(() -> dossier(suivi, travail, LE_10_MAI_2026_A_21H.minusNanos(1))).isExactlyInstanceOf(
      FinAutomatiqueIntrouvableException.class
    );
  }

  @Test
  void shouldLireLActiviteEchueQueLePointageOuvre() {
    var travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var suivi = suiviDAtelierEngage().enregistre(travail);

    var dossier = dossier(suivi, travail, LE_10_MAI_2026_A_22H);

    assertThat(dossier.adresse().pointage()).isEqualTo(travail.id());
    assertThat(dossier.activite().activite()).isEqualTo(travail.activite().orElseThrow());
    assertThat(dossier.activite().debut()).isEqualTo(LE_10_MAI_2026_A_8H);
    assertThat(dossier.activite().fin()).contains(LE_10_MAI_2026_A_21H);
    assertThat(dossier.lecture().suivi().revision()).isEqualTo(suivi.revision());
  }

  @Test
  void shouldNeLirQueLesPointagesDeLaCleDeLActivite() {
    var travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var autrePoste = debutSurFraiseuse2ParDupontA(LE_10_MAI_2026_A_9H);
    var autreOperateur = debutSurFraiseuse1ParMartinA(LE_10_MAI_2026_A_9H);
    var finDuTravailDeLaCle = finDe(travail).a(LE_10_MAI_2026_A_12H);
    var relance = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_13H);
    var suivi = suiviDAtelierEngage()
      .enregistre(travail)
      .enregistre(autrePoste)
      .enregistre(autreOperateur)
      .enregistre(finDuTravailDeLaCle)
      .enregistre(relance);

    var dossier = dossier(suivi, relance, LE_11_MAI_2026_A_8H);

    assertThat(dossier.pointages()).containsExactly(travail, finDuTravailDeLaCle, relance);
  }

  @Test
  void shouldRefuserUnPointageQuiNOuvreAucuneActiviteEchue() {
    var travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var fin = finDe(travail).a(LE_10_MAI_2026_A_17H);
    var suivi = suiviDAtelierEngage().enregistre(travail).enregistre(fin);

    assertThatThrownBy(() -> dossier(suivi, fin, LE_10_MAI_2026_A_23H)).isExactlyInstanceOf(FinAutomatiqueIntrouvableException.class);
    assertThatThrownBy(() -> dossier(suivi, travail, LE_10_MAI_2026_A_23H)).isExactlyInstanceOf(FinAutomatiqueIntrouvableException.class);
  }

  @Test
  void shouldRefuserUnPointageInconnuDuSuivi() {
    var suivi = suiviDAtelierEngage().enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H));
    var inconnu = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);

    assertThatThrownBy(() -> dossier(suivi, inconnu, LE_10_MAI_2026_A_23H)).isExactlyInstanceOf(FinAutomatiqueIntrouvableException.class);
  }

  @Test
  void shouldRefuserUneFinAutomatiqueDejaRegularisee() {
    var travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var suivi = suiviDAtelierEngage().enregistre(travail).enregistre(finRegulariseeParLeroyDe(travail).a(LE_10_MAI_2026_A_17H));

    assertThatThrownBy(() -> dossier(suivi, travail, LE_10_MAI_2026_A_23H)).isExactlyInstanceOf(FinAutomatiqueIntrouvableException.class);
  }

  private static LectureDossierAnomalie dossier(SuiviDAtelier suivi, EvenementDAtelier ouvrant, Instant evaluation) {
    return LectureDossierAnomalie.de(new AdresseDossierAnomalie(suivi.id(), ouvrant.id()), new LectureDuSuivi(suivi, evaluation));
  }
}
