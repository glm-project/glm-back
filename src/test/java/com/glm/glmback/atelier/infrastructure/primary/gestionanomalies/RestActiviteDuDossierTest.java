package com.glm.glmback.atelier.infrastructure.primary.gestionanomalies;

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

    assertThat(json.path("duree").asString()).isEqualTo("PT13H");
    assertThat(json.path("fin").asString()).isEqualTo("2026-05-10T21:00:00Z");
  }
}
