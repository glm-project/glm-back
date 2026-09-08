package com.glm.glmback.coutderevient.domain;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.NumberValueTooLowException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

@UnitTest
class MontantTest {

  @Test
  void shouldNotBuildWithoutValue() {
    assertThatThrownBy(() -> new Montant(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("montant");
  }

  @Test
  void shouldNotBuildWithNegativeValue() {
    assertThatThrownBy(() -> new Montant(new BigDecimal("-0.01")))
      .isExactlyInstanceOf(NumberValueTooLowException.class)
      .hasMessageContaining("montant");
  }

  /**
   * C'est ici, et nulle part ailleurs, que le calcul quitte son echelle de travail : un montant est un montant, donc
   * deux decimales.
   */
  @Test
  void shouldRoundToTwoDecimals() {
    assertThat(new Montant(new BigDecimal("12.345")).value()).isEqualTo(new BigDecimal("12.35"));
  }

  @Test
  void shouldAddMontant() {
    assertThat(new Montant(new BigDecimal("12.35")).plus(new Montant(new BigDecimal("1.65")))).isEqualTo(
      new Montant(new BigDecimal("14.00"))
    );
  }

  @Test
  void shouldBeZero() {
    assertThat(Montant.ZERO.value()).isEqualByComparingTo(BigDecimal.ZERO);
  }
}
