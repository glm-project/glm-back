package com.glm.glmback.naturedetravail.domain;

import static com.glm.glmback.naturedetravail.domain.NaturesDeTravailFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import org.junit.jupiter.api.Test;

@UnitTest
class NatureDeTravailListeeTest {

  @Test
  void shouldNotBuildWithoutNature() {
    assertThatThrownBy(() -> new NatureDeTravailListee(null, false))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("nature");
  }

  @Test
  void shouldBuildNatureDeTravailListee() {
    NatureDeTravailListee listee = new NatureDeTravailListee(natureDeTravailSoudage(), true);

    assertThat(listee.nature()).isEqualTo(natureDeTravailSoudage());
    assertThat(listee.utilisee()).isTrue();
  }
}
