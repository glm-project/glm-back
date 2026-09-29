package com.glm.glmback.feuilledetemps.domain;

import static com.glm.glmback.feuilledetemps.domain.FeuilleDeTempsFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class IntervalleDActiviteTest {

  private static final Plage DE_8H_A_12H = new Plage(LE_LUNDI_11_MAI_2026_A_8H, Optional.of(LE_LUNDI_11_MAI_2026_A_12H));

  @Test
  void shouldNotBuildWithoutActivite() {
    assertThatThrownBy(() -> new IntervalleDActivite(null, DE_8H_A_12H))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("activite");
  }

  @Test
  void shouldNotBuildWithoutPlage() {
    assertThatThrownBy(() -> new IntervalleDActivite(activiteDeTravailDuCarterSurLaDmu50(), null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("plage");
  }
}
