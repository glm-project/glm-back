package com.glm.glmback.atelier.domain;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@UnitTest
class JourneeDeTravailIdTest {

  private static final JourneeDeTravailId PLUS_PETIT = new JourneeDeTravailId(new UUID(0, 1));
  private static final JourneeDeTravailId PLUS_GRAND = new JourneeDeTravailId(new UUID(0, 2));

  @Test
  void shouldNotBuildWithoutUuid() {
    assertThatThrownBy(() -> new JourneeDeTravailId(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("id de la journee de travail");
  }

  @Test
  void shouldBuildNewIdWithRandomUuid() {
    assertThat(JourneeDeTravailId.newId().uuid()).isNotNull().isNotEqualTo(JourneeDeTravailId.newId().uuid());
  }

  @Test
  void shouldOrderById() {
    assertThat(PLUS_PETIT).isLessThan(PLUS_GRAND);
    assertThat(PLUS_GRAND).isGreaterThan(PLUS_PETIT);
    assertThat(PLUS_PETIT).isEqualByComparingTo(new JourneeDeTravailId(new UUID(0, 1)));
  }
}
