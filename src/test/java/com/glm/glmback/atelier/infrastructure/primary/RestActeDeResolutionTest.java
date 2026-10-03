package com.glm.glmback.atelier.infrastructure.primary;

import static com.glm.glmback.atelier.domain.ActeDeResolutionFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

@UnitTest
class RestActeDeResolutionTest {

  @Test
  void shouldEchoLaCorrectionAvecLInstantExactementDemande() {
    var correction = correctionDeLaFinAvecNeufDecimales();
    var json = JsonMapper.builder().build().valueToTree(RestActeDeResolution.from(correction));
    assertThat(json.path("kind").asString()).isEqualTo("CORRECTION");
    assertThat(json.path("pointage").asString()).isEqualTo(correction.commande().evenement().uuid().toString());
    assertThat(json.at("/fait/instant").asString()).isEqualTo("2026-05-10T19:00:00.123456789+02:00");
    assertThat(json.at("/fait/activiteVisee").asString()).isEqualTo(
      correction.commande().remplacement().activiteVisee().orElseThrow().uuid().toString()
    );
    assertThat(json.has("auteur")).isFalse();
  }
}
