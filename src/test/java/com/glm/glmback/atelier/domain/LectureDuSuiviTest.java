package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

@UnitTest
class LectureDuSuiviTest {

  @Test
  void shouldNotBuildWithoutSuivi() {
    assertThatThrownBy(() -> new LectureDuSuivi(null, LE_10_MAI_2026_A_17H))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("suivi");
  }

  @Test
  void shouldNotBuildWithoutEvaluation() {
    SuiviDAtelier suivi = suiviDAtelierEngage();

    assertThatThrownBy(() -> new LectureDuSuivi(suivi, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("evaluation");
  }

  /**
   * Le meme suivi, lu avant puis apres l'echeance de son activite : en cours a 20:59, interrompu a 21:00.
   */
  @Test
  void shouldJugerLEcheanceALInstantDeLaLecture() {
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H));

    LectureDuSuivi avant = new LectureDuSuivi(suivi, Instant.parse("2026-05-10T20:59:00Z"));
    LectureDuSuivi apres = new LectureDuSuivi(suivi, Instant.parse("2026-05-10T21:00:00Z"));

    assertThat(avant.etat()).isEqualTo(EtatDAtelier.EN_COURS);
    assertThat(avant.activitesEnCours()).hasSize(1);
    assertThat(apres.etat()).isEqualTo(EtatDAtelier.INTERROMPU);
    assertThat(apres.activitesEnCours()).isEmpty();
  }

  /**
   * Les sequences en conflit ne dependent pas de l'instant de lecture : ce sont celles du suivi.
   */
  @Test
  void shouldLireLesSequencesEnConflitDuSuivi() {
    EvenementDAtelier premiere = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    SuiviDAtelier suivi = suiviDAtelierEngage()
      .enregistre(premiere)
      .enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_9H))
      .enregistre(finDe(premiere).a(LE_10_MAI_2026_A_12H));

    assertThat(new LectureDuSuivi(suivi, LE_10_MAI_2026_A_17H).conflits()).hasSize(1).isEqualTo(suivi.conflits());
  }
}
