package com.glm.glmback.atelier.domain;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@UnitTest
class EvenementDAtelierIdTest {

  private static final EvenementDAtelierId PLUS_PETIT = new EvenementDAtelierId(new UUID(0, 1));
  private static final EvenementDAtelierId PLUS_GRAND = new EvenementDAtelierId(new UUID(0, 2));

  @Test
  void shouldNotBuildWithoutUuid() {
    assertThatThrownBy(() -> new EvenementDAtelierId(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("id de l'evenement d'atelier");
  }

  @Test
  void shouldBuildNewIdWithRandomUuid() {
    assertThat(EvenementDAtelierId.newId().uuid()).isNotNull().isNotEqualTo(EvenementDAtelierId.newId().uuid());
  }

  @Test
  void shouldOrderById() {
    assertThat(PLUS_PETIT).isLessThan(PLUS_GRAND);
    assertThat(PLUS_GRAND).isGreaterThan(PLUS_PETIT);
    assertThat(PLUS_PETIT).isEqualByComparingTo(new EvenementDAtelierId(new UUID(0, 1)));
  }
}
