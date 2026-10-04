package com.glm.glmback.postedetravail.domain;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.NumberValueTooHighException;
import com.glm.glmback.shared.error.domain.NumberValueTooLowException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@UnitTest
class CoutHoraireTest {

  @ParameterizedTest
  @ValueSource(strings = { "0.001", "45.555", "100000000", "1E+8" })
  void shouldRefuserUnCoutNonRepresentableEnCentimes(String valeur) {
    assertThatThrownBy(() -> new CoutHoraire(new BigDecimal(valeur)))
      .isExactlyInstanceOf(NumberValueTooHighException.class)
      .hasMessageContaining("cout horaire");
  }

  @Test
  void shouldNotBuildWithoutValue() {
    assertThatThrownBy(() -> new CoutHoraire(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("cout horaire");
  }

  @Test
  void shouldNotBuildWithZeroValue() {
    assertThatThrownBy(() -> new CoutHoraire(BigDecimal.ZERO))
      .isExactlyInstanceOf(NumberValueTooLowException.class)
      .hasMessageContaining("cout horaire");
  }

  @Test
  void shouldNotBuildWithNegativeValue() {
    assertThatThrownBy(() -> new CoutHoraire(new BigDecimal("-1")))
      .isExactlyInstanceOf(NumberValueTooLowException.class)
      .hasMessageContaining("cout horaire");
  }

  @Test
  void shouldGetValueFromValidCoutHoraire() {
    assertThat(new CoutHoraire(new BigDecimal("45.50")).value()).isEqualTo(new BigDecimal("45.50"));
  }

  @Test
  void shouldNotBuildOptionalCoutHoraireFromNull() {
    assertThat(CoutHoraire.of(null)).isEmpty();
  }

  @Test
  void shouldBuildOptionalCoutHoraireFromValue() {
    assertThat(CoutHoraire.of(new BigDecimal("45.50"))).contains(new CoutHoraire(new BigDecimal("45.50")));
  }
}
