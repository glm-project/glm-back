package com.glm.glmback.elementdefabrication.domain;

import static com.glm.glmback.elementdefabrication.domain.ElementsDeFabricationFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.NotAfterTimeException;
import org.junit.jupiter.api.Test;

@UnitTest
class PeriodeTest {

  @Test
  void shouldNotBuildWithoutDebut() {
    assertThatThrownBy(() -> new Periode(null, LE_31_MARS_2026))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("debut");
  }

  @Test
  void shouldNotBuildWithoutFin() {
    assertThatThrownBy(() -> new Periode(LE_1ER_JANVIER_2026, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("fin");
  }

  @Test
  void shouldNotBuildWithFinBeforeDebut() {
    assertThatThrownBy(() -> new Periode(LE_31_MARS_2026, LE_1ER_JANVIER_2026))
      .isExactlyInstanceOf(NotAfterTimeException.class)
      .hasMessageContaining("fin");
  }

  @Test
  void shouldNotContainDateBeforeDebut() {
    assertThat(premierTrimestre2026().contains(LE_1ER_JANVIER_2026.minusSeconds(1))).isFalse();
  }

  @Test
  void shouldNotContainDateAfterFin() {
    assertThat(premierTrimestre2026().contains(LE_31_MARS_2026.plusSeconds(1))).isFalse();
  }

  @Test
  void shouldContainDateBetweenDebutAndFin() {
    assertThat(premierTrimestre2026().contains(LE_15_JANVIER_2026)).isTrue();
  }

  @Test
  void shouldContainBounds() {
    assertThat(premierTrimestre2026().contains(LE_1ER_JANVIER_2026)).isTrue();
    assertThat(premierTrimestre2026().contains(LE_31_MARS_2026)).isTrue();
  }
}
