package com.glm.glmback.atelier.infrastructure.primary;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

@UnitTest
class RestActiviteDuDossierTest {

  @Test
  void shouldPublierLEcheanceDeLInterpretationSansInventerDeFinReelle() {
    var debut = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var suivi = suiviDAtelierEngage().enregistre(debut);
    var intervalle = suivi.activites().getFirst().a(LE_10_MAI_2026_A_17H.plusSeconds(5 * 3600));

    var json = JsonMapper.builder().build().valueToTree(RestActiviteDuDossier.from(intervalle, annuaireDeDupontEtMartin()));

    assertThat(json.path("etat").asString()).isEqualTo("ECHUE");
    assertThat(json.path("duree").asString()).isEqualTo("PT13H");
    assertThat(json.path("fin").asString()).isEqualTo("2026-05-10T21:00:00Z");
  }

  @Test
  void shouldPublierUneDureeIsoPourUneActiviteTerminee() {
    var debut = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var suivi = suiviDAtelierEngage().enregistre(debut).enregistre(finDe(debut).a(LE_10_MAI_2026_A_17H));
    var intervalle = suivi.activites().getFirst().a(LE_10_MAI_2026_A_17H);

    var json = JsonMapper.builder().build().valueToTree(RestActiviteDuDossier.from(intervalle, annuaireDeDupontEtMartin()));

    assertThat(json.path("etat").asString()).isEqualTo("TERMINEE");
    assertThat(json.path("duree").asString()).isEqualTo("PT9H");
    assertThat(json.path("fin").asString()).isEqualTo("2026-05-10T17:00:00Z");
  }
}
