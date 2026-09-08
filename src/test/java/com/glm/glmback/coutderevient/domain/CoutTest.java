package com.glm.glmback.coutderevient.domain;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

@UnitTest
class CoutTest {

  private static final Montant QUARANTE_CINQ_EUROS = new Montant(new BigDecimal("45.00"));
  private static final Montant VINGT_EUROS = new Montant(new BigDecimal("20.00"));

  @Test
  void shouldNotBuildWithoutMachine() {
    assertThatThrownBy(() -> new Cout(null, VINGT_EUROS))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("cout machine");
  }

  @Test
  void shouldNotBuildWithoutMainDOeuvre() {
    assertThatThrownBy(() -> new Cout(QUARANTE_CINQ_EUROS, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("cout de main d'oeuvre");
  }

  @Test
  void shouldAddBothParts() {
    assertThat(new Cout(QUARANTE_CINQ_EUROS, VINGT_EUROS).total()).isEqualTo(new Montant(new BigDecimal("65.00")));
  }

  @Test
  void shouldSumCouts() {
    Cout somme = new Cout(QUARANTE_CINQ_EUROS, VINGT_EUROS).plus(new Cout(VINGT_EUROS, VINGT_EUROS));

    assertThat(somme).isEqualTo(new Cout(new Montant(new BigDecimal("65.00")), new Montant(new BigDecimal("40.00"))));
  }

  @Test
  void shouldStartFromAucun() {
    assertThat(Cout.AUCUN.total()).isEqualTo(Montant.ZERO);
  }
}
