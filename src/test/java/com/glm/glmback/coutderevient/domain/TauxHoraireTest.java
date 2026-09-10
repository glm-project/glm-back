package com.glm.glmback.coutderevient.domain;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.NumberValueTooLowException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

@UnitTest
class TauxHoraireTest {

  @Test
  void shouldNotBuildWithoutValue() {
    assertThatThrownBy(() -> new TauxHoraire(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("taux horaire");
  }

  @Test
  void shouldNotBuildWithZero() {
    assertThatThrownBy(() -> new TauxHoraire(BigDecimal.ZERO))
      .isExactlyInstanceOf(NumberValueTooLowException.class)
      .hasMessageContaining("taux horaire");
  }

  @Test
  void shouldBuildWithValue() {
    assertThat(new TauxHoraire(new BigDecimal("20.00")).value()).isEqualByComparingTo("20.00");
  }

  @Test
  void shouldNotBuildOptionalFromNull() {
    assertThat(TauxHoraire.of(null)).isEmpty();
  }

  @Test
  void shouldBuildOptionalFromValue() {
    assertThat(TauxHoraire.of(new BigDecimal("20.00"))).contains(new TauxHoraire(new BigDecimal("20.00")));
  }
}
