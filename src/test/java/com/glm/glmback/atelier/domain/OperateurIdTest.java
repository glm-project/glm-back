package com.glm.glmback.atelier.domain;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@UnitTest
class OperateurIdTest {

  @Test
  void shouldNotBuildWithoutUuid() {
    assertThatThrownBy(() -> new OperateurId(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("id de l'operateur");
  }

  @Test
  void shouldGetUuidFromValidOperateurId() {
    UUID uuid = UUID.randomUUID();

    assertThat(new OperateurId(uuid).uuid()).isEqualTo(uuid);
  }
}
