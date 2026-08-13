package com.glm.glmback.atelier.domain;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@UnitTest
class PosteDeTravailIdTest {

  @Test
  void shouldNotBuildWithoutUuid() {
    assertThatThrownBy(() -> new PosteDeTravailId(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("id du poste de travail");
  }

  @Test
  void shouldGetUuidFromValidPosteDeTravailId() {
    UUID uuid = UUID.randomUUID();

    assertThat(new PosteDeTravailId(uuid).uuid()).isEqualTo(uuid);
  }
}
