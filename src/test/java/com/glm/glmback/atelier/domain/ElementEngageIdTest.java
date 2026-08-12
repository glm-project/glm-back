package com.glm.glmback.atelier.domain;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@UnitTest
class ElementEngageIdTest {

  private static final ElementEngageId PLUS_PETIT = new ElementEngageId(new UUID(0, 1));
  private static final ElementEngageId PLUS_GRAND = new ElementEngageId(new UUID(0, 2));

  @Test
  void shouldNotBuildWithoutUuid() {
    assertThatThrownBy(() -> new ElementEngageId(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("id de l'element engage");
  }

  @Test
  void shouldOrderById() {
    assertThat(PLUS_PETIT).isLessThan(PLUS_GRAND);
    assertThat(PLUS_GRAND).isGreaterThan(PLUS_PETIT);
    assertThat(PLUS_PETIT).isEqualByComparingTo(new ElementEngageId(new UUID(0, 1)));
  }
}
