package com.glm.glmback.atelier.domain;

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
  void shouldNotBuildWithZeroValue() {
    assertThatThrownBy(() -> new TauxHoraire(BigDecimal.ZERO))
      .isExactlyInstanceOf(NumberValueTooLowException.class)
      .hasMessageContaining("taux horaire");
  }

  @Test
  void shouldNotBuildWithNegativeValue() {
    assertThatThrownBy(() -> new TauxHoraire(new BigDecimal("-1")))
      .isExactlyInstanceOf(NumberValueTooLowException.class)
      .hasMessageContaining("taux horaire");
  }

  @Test
  void shouldGetValueFromValidTauxHoraire() {
    assertThat(new TauxHoraire(new BigDecimal("22.00")).value()).isEqualTo(new BigDecimal("22.00"));
  }

  @Test
  void shouldNotBuildOptionalTauxHoraireFromNull() {
    assertThat(TauxHoraire.of(null)).isEmpty();
  }

  @Test
  void shouldBuildOptionalTauxHoraireFromValue() {
    assertThat(TauxHoraire.of(new BigDecimal("22.00"))).contains(new TauxHoraire(new BigDecimal("22.00")));
  }
}
