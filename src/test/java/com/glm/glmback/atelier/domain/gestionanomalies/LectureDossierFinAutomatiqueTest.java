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
class LectureDossierFinAutomatiqueTest {

  private static final Instant LE_10_MAI_2026_A_21H = Instant.parse("2026-05-10T21:00:00Z");
  private static final Instant LE_10_MAI_2026_A_21H30 = Instant.parse("2026-05-10T21:30:00Z");
  private static final Instant LE_10_MAI_2026_A_22H = Instant.parse("2026-05-10T22:00:00Z");
  private static final Instant LE_10_MAI_2026_A_23H = Instant.parse("2026-05-10T23:00:00Z");
  private static final Instant LE_10_MAI_2026_A_23H30 = Instant.parse("2026-05-10T23:30:00Z");

  @Test
  void shouldOuvrirUnDossierDeFinAutomatiqueExactementALEcheance() {
    var travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var suivi = suiviDAtelierEngage().enregistre(travail);

    assertThat(dossier(suivi, travail, LE_10_MAI_2026_A_21H).kind()).isEqualTo(EtatDAdresseDossier.FIN_AUTOMATIQUE);
    assertThat(dossier(suivi, travail, LE_10_MAI_2026_A_21H.minusNanos(1)).kind()).isEqualTo(EtatDAdresseDossier.SANS_ANOMALIE);
  }

  @Test
  void shouldConcernerLActiviteEchueDeLAncreSansSequence() {
    var travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var suivi = suiviDAtelierEngage().enregistre(travail);

    var dossier = dossier(suivi, travail, LE_10_MAI_2026_A_22H);

    assertThat(dossier.sequence()).isEmpty();
    assertThat(dossier.concernees()).containsExactly(travail.activite().orElseThrow());
    assertThat(dossier.finAutomatique()).isTrue();
    assertThat(dossier.enConflit()).isFalse();
    assertThat(dossier.activites())
      .singleElement()
      .satisfies(activite -> {
        assertThat(activite.finAutomatique()).isTrue();
        assertThat(activite.fin()).contains(LE_10_MAI_2026_A_21H);
      });
    assertThat(dossier.perimetre())
      .get()
      .satisfies(perimetre -> {
        assertThat(perimetre.cle()).isEqualTo(travail.cle());
        assertThat(perimetre.activites()).containsExactly(travail.activite().orElseThrow());
        assertThat(perimetre.pointages()).containsExactly(travail.id());
      });
  }

  @Test
  void shouldNeRienConcernerQuandLActiviteDeLAncreEstEncoreEnCours() {
    var travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var suivi = suiviDAtelierEngage().enregistre(travail);

    var dossier = dossier(suivi, travail, LE_10_MAI_2026_A_20H);

    assertThat(dossier.concernees()).isEmpty();
    assertThat(dossier.finAutomatique()).isFalse();
    assertThat(dossier.activites()).isEmpty();
  }

  @Test
  void shouldNeRienConcernerQuandLAncreEstUneFinActive() {
    var travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var fin = finDe(travail).a(LE_10_MAI_2026_A_17H);
    var suivi = suiviDAtelierEngage().enregistre(travail).enregistre(fin);

    var dossier = dossier(suivi, fin, LE_10_MAI_2026_A_23H);

    assertThat(dossier.kind()).isEqualTo(EtatDAdresseDossier.SANS_ANOMALIE);
    assertThat(dossier.concernees()).isEmpty();
    assertThat(dossier.finAutomatique()).isFalse();
  }

  @Test
  void shouldNePasOuvrirDeDossierDeFinAutomatiqueSurUneAncreAnnulee() {
    var travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var suivi = suiviDAtelierEngage().enregistre(travail).annule(travail.id(), annulationParLeroy());

    var dossier = dossier(suivi, travail, LE_10_MAI_2026_A_23H);

    assertThat(dossier.kind()).isEqualTo(EtatDAdresseDossier.ANCRE_ANNULEE);
    assertThat(dossier.concernees()).isEmpty();
    assertThat(dossier.finAutomatique()).isFalse();
  }

  private static LectureDossierAnomalie dossier(SuiviDAtelier suivi, EvenementDAtelier ancre, Instant evaluation) {
    return new LectureDossierAnomalie(new AdresseDossierAnomalie(suivi.id(), ancre.id()), new LectureDuSuivi(suivi, evaluation));
  }
}
