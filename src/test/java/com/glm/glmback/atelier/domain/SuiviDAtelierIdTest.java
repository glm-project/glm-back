package com.glm.glmback.atelier.domain;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@UnitTest
class SuiviDAtelierIdTest {

  private static final SuiviDAtelierId PLUS_PETIT = new SuiviDAtelierId(new UUID(0, 1));
  private static final SuiviDAtelierId PLUS_GRAND = new SuiviDAtelierId(new UUID(0, 2));

  @Test
  void shouldNotBuildWithoutUuid() {
    assertThatThrownBy(() -> new SuiviDAtelierId(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("id du suivi d'atelier");
  }

  @Test
  void shouldBuildNewIdWithRandomUuid() {
    assertThat(SuiviDAtelierId.newId().uuid()).isNotNull().isNotEqualTo(SuiviDAtelierId.newId().uuid());
  }

  @Test
  void shouldOrderById() {
    assertThat(PLUS_PETIT).isLessThan(PLUS_GRAND);
    assertThat(PLUS_GRAND).isGreaterThan(PLUS_PETIT);
    assertThat(PLUS_PETIT).isEqualByComparingTo(new SuiviDAtelierId(new UUID(0, 1)));
  }
}
