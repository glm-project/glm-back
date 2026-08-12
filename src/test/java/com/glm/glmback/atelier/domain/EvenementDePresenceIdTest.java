package com.glm.glmback.atelier.domain;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@UnitTest
class EvenementDePresenceIdTest {

  private static final EvenementDePresenceId PLUS_PETIT = new EvenementDePresenceId(new UUID(0, 1));
  private static final EvenementDePresenceId PLUS_GRAND = new EvenementDePresenceId(new UUID(0, 2));

  @Test
  void shouldNotBuildWithoutUuid() {
    assertThatThrownBy(() -> new EvenementDePresenceId(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("id de l'evenement de presence");
  }

  @Test
  void shouldBuildNewIdWithRandomUuid() {
    assertThat(EvenementDePresenceId.newId().uuid()).isNotNull().isNotEqualTo(EvenementDePresenceId.newId().uuid());
  }

  @Test
  void shouldOrderById() {
    assertThat(PLUS_PETIT).isLessThan(PLUS_GRAND);
    assertThat(PLUS_GRAND).isGreaterThan(PLUS_PETIT);
    assertThat(PLUS_PETIT).isEqualByComparingTo(new EvenementDePresenceId(new UUID(0, 1)));
  }
}
