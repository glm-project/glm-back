package com.glm.glmback.naturedetravail.domain;

import static com.glm.glmback.naturedetravail.domain.NaturesDeTravailFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.NumberValueTooLowException;
import org.junit.jupiter.api.Test;

@UnitTest
class NatureDeTravailListeeTest {

  @Test
  void shouldNotBuildWithoutNature() {
    assertThatThrownBy(() -> new NatureDeTravailListee(null, false, 0))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("nature");
  }

  @Test
  void shouldBuildNatureDeTravailListee() {
    NatureDeTravailListee listee = new NatureDeTravailListee(natureDeTravailSoudage(), true, 3);

    assertThat(listee.nature()).isEqualTo(natureDeTravailSoudage());
    assertThat(listee.utilisee()).isTrue();
    assertThat(listee.postes()).isEqualTo(3);
  }

  @Test
  void shouldNotBuildWithNegativePostes() {
    assertThatThrownBy(() -> new NatureDeTravailListee(natureDeTravailSoudage(), false, -1))
      .isExactlyInstanceOf(NumberValueTooLowException.class)
      .hasMessageContaining("postes");
  }
}
