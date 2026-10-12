package com.glm.glmback.elementdefabrication.domain;

import static com.glm.glmback.elementdefabrication.domain.ElementsDeFabricationFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class ElementsDansLeurCycleCriteriaTest {

  @Test
  void shouldMatchUnElementCreeDansLaPeriode() {
    var criteria = new ElementsDansLeurCycleCriteria(Optional.of(premierTrimestre2026()), INSTANT_10_MAI_2026_A_18H);

    assertThat(criteria.matches(enPreparation(elementDeFabricationOrdre1015CreeLe(LE_15_JANVIER_2026)))).isTrue();
  }

  @Test
  void shouldNotMatchUnElementCreeHorsDeLaPeriode() {
    var criteria = new ElementsDansLeurCycleCriteria(Optional.of(premierTrimestre2026()), INSTANT_10_MAI_2026_A_18H);

    assertThat(criteria.matches(enPreparation(elementDeFabricationOrdre1015CreeLe(LE_31_MARS_2026.plusSeconds(1))))).isFalse();
  }

  @Test
  void shouldMatchToutElementSansPeriode() {
    var criteria = new ElementsDansLeurCycleCriteria(Optional.empty(), INSTANT_10_MAI_2026_A_18H);

    assertThat(criteria.matches(enPreparation(elementDeFabricationOrdre1015CreeLe(LE_31_MARS_2026.plusSeconds(1))))).isTrue();
  }

  @Test
  void shouldNotBuildWithoutCreation() {
    assertThatThrownBy(() -> new ElementsDansLeurCycleCriteria(null, INSTANT_10_MAI_2026_A_18H))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("creation");
  }

  @Test
  void shouldNotBuildWithoutEvaluation() {
    assertThatThrownBy(() -> new ElementsDansLeurCycleCriteria(Optional.empty(), null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("evaluation");
  }

  private static ElementDansSonCycle enPreparation(ElementDeFabrication element) {
    return new ElementDansSonCycle(element, Optional.empty());
  }
}
