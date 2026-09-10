package com.glm.glmback.coutderevient.domain;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@UnitTest
class ElementIdTest {

  @Test
  void shouldNotBuildWithoutUuid() {
    assertThatThrownBy(() -> new ElementId(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("id de l'element de fabrication");
  }

  @Test
  void shouldBuildWithUuid() {
    UUID uuid = UUID.fromString("11111111-1111-1111-1111-111111111111");

    assertThat(new ElementId(uuid).uuid()).isEqualTo(uuid);
  }
}
