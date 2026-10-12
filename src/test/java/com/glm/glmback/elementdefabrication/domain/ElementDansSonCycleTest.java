package com.glm.glmback.elementdefabrication.domain;

import static com.glm.glmback.elementdefabrication.domain.ElementsDeFabricationFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class ElementDansSonCycleTest {

  @Test
  void shouldEtreEnPreparationSansPassage() {
    assertThat(new ElementDansSonCycle(elementDeFabricationOrdre1015(), Optional.empty()).cycle()).isEqualTo(CycleDeVie.EN_PREPARATION);
  }

  @Test
  void shouldEtreEnAtelierSurUnPassageOuvert() {
    var lu = new ElementDansSonCycle(elementDeFabricationOrdre1015(), Optional.of(passageEnCoursDepuisLe10Mai2026()));

    assertThat(lu.cycle()).isEqualTo(CycleDeVie.EN_ATELIER);
  }

  @Test
  void shouldEtreClotureSurUnPassageClos() {
    var lu = new ElementDansSonCycle(elementDeFabricationOrdre1015(), Optional.of(passageClosParLeroyLe10Mai2026()));

    assertThat(lu.cycle()).isEqualTo(CycleDeVie.CLOTURE);
  }

  @Test
  void shouldNotBuildWithoutElement() {
    assertThatThrownBy(() -> new ElementDansSonCycle(null, Optional.empty()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("element");
  }

  @Test
  void shouldNotBuildWithoutPassage() {
    assertThatThrownBy(() -> new ElementDansSonCycle(elementDeFabricationOrdre1015(), null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("passage");
  }
}
