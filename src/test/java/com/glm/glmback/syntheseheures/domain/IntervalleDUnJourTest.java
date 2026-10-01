package com.glm.glmback.syntheseheures.domain;

import static com.glm.glmback.syntheseheures.domain.SyntheseHeuresFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class IntervalleDUnJourTest {

  private static final IntervalleDActivite TRAVAIL_DE_8H_A_12H = new IntervalleDActivite(
    activiteDeTravailDuCarterSurLaDmu50(),
    new Plage(LE_LUNDI_11_MAI_2026_A_8H, Optional.of(LE_LUNDI_11_MAI_2026_A_12H)),
    travailDuCarterLuSur(new Plage(LE_LUNDI_11_MAI_2026_A_8H, Optional.of(LE_LUNDI_11_MAI_2026_A_12H)))
  );

  @Test
  void shouldNotBuildWithoutJour() {
    assertThatThrownBy(() -> new IntervalleDUnJour(null, TRAVAIL_DE_8H_A_12H))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("jour");
  }

  @Test
  void shouldNotBuildWithoutIntervalle() {
    assertThatThrownBy(() -> new IntervalleDUnJour(LUNDI_11_MAI_2026, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("intervalle");
  }
}
