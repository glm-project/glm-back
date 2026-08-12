package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.NotAfterTimeException;
import org.junit.jupiter.api.Test;

@UnitTest
class PeriodeTest {

  @Test
  void shouldNotBuildWithoutDebut() {
    assertThatThrownBy(() -> new Periode(null, LE_10_MAI_2026_A_17H))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("debut");
  }

  @Test
  void shouldNotBuildWithoutFin() {
    assertThatThrownBy(() -> new Periode(LE_10_MAI_2026_A_7H, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("fin");
  }

  @Test
  void shouldNotBuildWithFinBeforeDebut() {
    assertThatThrownBy(() -> new Periode(LE_10_MAI_2026_A_17H, LE_10_MAI_2026_A_7H))
      .isExactlyInstanceOf(NotAfterTimeException.class)
      .hasMessageContaining("fin");
  }

  @Test
  void shouldNotContainDateBeforeDebut() {
    assertThat(journeeDu10Mai2026().contains(LE_10_MAI_2026_A_7H.minusSeconds(1))).isFalse();
  }

  @Test
  void shouldNotContainDateAfterFin() {
    assertThat(journeeDu10Mai2026().contains(LE_10_MAI_2026_A_17H.plusSeconds(1))).isFalse();
  }

  @Test
  void shouldContainDateBetweenDebutAndFin() {
    assertThat(journeeDu10Mai2026().contains(LE_10_MAI_2026_A_12H)).isTrue();
  }

  @Test
  void shouldContainBounds() {
    assertThat(journeeDu10Mai2026().contains(LE_10_MAI_2026_A_7H)).isTrue();
    assertThat(journeeDu10Mai2026().contains(LE_10_MAI_2026_A_17H)).isTrue();
  }
}
