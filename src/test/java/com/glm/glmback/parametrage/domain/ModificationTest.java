package com.glm.glmback.parametrage.domain;

import static com.glm.glmback.parametrage.domain.ParametrageFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import org.junit.jupiter.api.Test;

@UnitTest
class ModificationTest {

  @Test
  void shouldNotBuildWithoutAuteur() {
    assertThatThrownBy(() -> new Modification(null, LE_24_SEPTEMBRE_2026_A_9H))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("auteur");
  }

  @Test
  void shouldNotBuildWithoutDate() {
    assertThatThrownBy(() -> new Modification(AUTEUR_LEROY, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("date de modification");
  }
}
