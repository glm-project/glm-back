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
    var criteria = new ElementsDansLeurCycleCriteria(creeAuPremierTrimestre2026(), Optional.empty(), INSTANT_10_MAI_2026_A_18H);

    assertThat(criteria.matches(enPreparation(elementDeFabricationOrdre1015CreeLe(LE_15_JANVIER_2026)))).isTrue();
  }

  @Test
  void shouldNotMatchUnElementCreeHorsDeLaPeriode() {
    var criteria = new ElementsDansLeurCycleCriteria(creeAuPremierTrimestre2026(), Optional.empty(), INSTANT_10_MAI_2026_A_18H);

    assertThat(criteria.matches(enPreparation(elementDeFabricationOrdre1015CreeLe(LE_31_MARS_2026.plusSeconds(1))))).isFalse();
  }

  @Test
  void shouldMatchToutElementSansPeriode() {
    var criteria = new ElementsDansLeurCycleCriteria(FiltreDesElements.aucun(), Optional.empty(), INSTANT_10_MAI_2026_A_18H);

    assertThat(criteria.matches(enPreparation(elementDeFabricationOrdre1015CreeLe(LE_31_MARS_2026.plusSeconds(1))))).isTrue();
  }

  @Test
  void shouldMatchUnElementDuCycleDemande() {
    var criteria = new ElementsDansLeurCycleCriteria(
      FiltreDesElements.aucun(),
      Optional.of(CycleDeVie.EN_ATELIER),
      INSTANT_10_MAI_2026_A_18H
    );

    assertThat(
      criteria.matches(new ElementDansSonCycle(elementDeFabricationOrdre1015(), Optional.of(passageEnCoursDepuisLe10Mai2026())))
    ).isTrue();
  }

  @Test
  void shouldNotMatchUnElementDUnAutreCycle() {
    var criteria = new ElementsDansLeurCycleCriteria(
      FiltreDesElements.aucun(),
      Optional.of(CycleDeVie.EN_ATELIER),
      INSTANT_10_MAI_2026_A_18H
    );

    assertThat(criteria.matches(enPreparation(elementDeFabricationOrdre1015()))).isFalse();
  }

  @Test
  void shouldNotBuildWithoutCycle() {
    assertThatThrownBy(() -> new ElementsDansLeurCycleCriteria(FiltreDesElements.aucun(), null, INSTANT_10_MAI_2026_A_18H))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("cycle");
  }

  @Test
  void shouldNotBuildWithoutFiltre() {
    assertThatThrownBy(() -> new ElementsDansLeurCycleCriteria(null, Optional.empty(), INSTANT_10_MAI_2026_A_18H))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("filtre");
  }

  @Test
  void shouldNotBuildWithoutEvaluation() {
    assertThatThrownBy(() -> new ElementsDansLeurCycleCriteria(FiltreDesElements.aucun(), Optional.empty(), null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("evaluation");
  }

  private static FiltreDesElements creeAuPremierTrimestre2026() {
    return new FiltreDesElements(Optional.of(premierTrimestre2026()), Optional.empty(), Optional.empty());
  }

  private static ElementDansSonCycle enPreparation(ElementDeFabrication element) {
    return new ElementDansSonCycle(element, Optional.empty());
  }
}
