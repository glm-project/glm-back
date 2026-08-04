package com.glm.glmback.elementdefabrication.domain;

import static com.glm.glmback.elementdefabrication.domain.ElementsDeFabricationFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import org.junit.jupiter.api.Test;

@UnitTest
class ElementDeFabricationCriteriaTest {

  @Test
  void shouldNotBuildWithoutPeriode() {
    assertThatThrownBy(() -> new ElementDeFabricationCriteria(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("periode");
  }

  @Test
  void shouldMatchElementCreatedInPeriode() {
    assertThat(criteresPremierTrimestre2026().matches(ordreDeFabricationAssemblageCarterCreeLe(LE_15_JANVIER_2026))).isTrue();
  }

  @Test
  void shouldNotMatchElementCreatedOutOfPeriode() {
    assertThat(criteresPremierTrimestre2026().matches(ordreDeFabricationAssemblageCarterCreeLe(LE_31_MARS_2026.plusSeconds(1)))).isFalse();
  }
}
