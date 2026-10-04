package com.glm.glmback.operateur.domain;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.NumberValueTooHighException;
import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@UnitTest
class TauxHoraireTest {

  @ParameterizedTest
  @ValueSource(strings = { "0.001", "22.555", "100000000", "1E+8" })
  void shouldRefuserUnTauxNonRepresentableEnCentimes(String valeur) {
    assertThatThrownBy(() -> new TauxHoraire(new BigDecimal(valeur)))
      .isExactlyInstanceOf(NumberValueTooHighException.class)
      .hasMessageContaining("taux horaire");
  }
}
