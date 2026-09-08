package com.glm.glmback.coutderevient.domain;

import static com.glm.glmback.coutderevient.domain.CoutDeRevientFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.NotAfterTimeException;
import java.time.Duration;
import org.junit.jupiter.api.Test;

@UnitTest
class PeriodeTest {

  @Test
  void shouldNotBuildWithoutDebut() {
    assertThatThrownBy(() -> new Periode(null, LE_11_MAI_A_9H))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("debut");
  }

  @Test
  void shouldNotBuildWithFinBeforeDebut() {
    assertThatThrownBy(() -> new Periode(LE_11_MAI_A_10H, LE_11_MAI_A_9H))
      .isExactlyInstanceOf(NotAfterTimeException.class)
      .hasMessageContaining("fin");
  }

  @Test
  void shouldMeasureDuree() {
    assertThat(new Periode(LE_11_MAI_A_9H, LE_11_MAI_A_10H).duree()).isEqualTo(Duration.ofHours(1));
  }

  @Test
  void shouldIntersectOverlappingPeriodes() {
    Periode intersection = new Periode(LE_11_MAI_A_9H, LE_11_MAI_A_11H)
      .intersection(new Periode(LE_11_MAI_A_10H, LE_11_MAI_A_12H))
      .orElseThrow();

    assertThat(intersection).isEqualTo(new Periode(LE_11_MAI_A_10H, LE_11_MAI_A_11H));
  }

  /**
   * Deux periodes qui ne font que se toucher ne partagent aucune duree : produire un instant valorisable a zero
   * ouvrirait une ligne de rapport sur du vide.
   */
  @Test
  void shouldNotIntersectAdjacentPeriodes() {
    assertThat(new Periode(LE_11_MAI_A_9H, LE_11_MAI_A_10H).intersection(new Periode(LE_11_MAI_A_10H, LE_11_MAI_A_11H))).isEmpty();
  }

  @Test
  void shouldNotIntersectDisjointPeriodes() {
    assertThat(new Periode(LE_11_MAI_A_9H, LE_11_MAI_A_10H).intersection(new Periode(LE_11_MAI_A_11H, LE_11_MAI_A_12H))).isEmpty();
  }
}
