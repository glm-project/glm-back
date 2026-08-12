package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import org.junit.jupiter.api.Test;

@UnitTest
class ClotureTest {

  @Test
  void shouldNotBuildWithoutAuteur() {
    assertThatThrownBy(() -> new Cloture(null, Horodatage.saisiA(LE_10_MAI_2026_A_17H)))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("auteur");
  }

  @Test
  void shouldNotBuildWithoutHorodatage() {
    assertThatThrownBy(() -> new Cloture(AUTEUR_LEROY, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("horodatage");
  }

  @Test
  void shouldReadDateDeSurvenueFromHorodatage() {
    assertThat(clotureParLeroyA(LE_10_MAI_2026_A_17H).dateDeSurvenue()).isEqualTo(LE_10_MAI_2026_A_17H);
    assertThat(clotureParLeroyA(LE_10_MAI_2026_A_17H).auteur()).isEqualTo(AUTEUR_LEROY);
  }
}
