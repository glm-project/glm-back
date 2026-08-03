package com.glm.glmback.elementdefabrication.domain;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@UnitTest
class OrdreDeFabricationIdTest {

  @Test
  void shouldNotBuildWithoutId() {
    assertThatThrownBy(() -> new OrdreDeFabricationId(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("id");
  }

  @Test
  void shouldGenerateNewId() {
    assertThat(OrdreDeFabricationId.newId()).isNotEqualTo(OrdreDeFabricationId.newId());
  }

  @Test
  void shouldGetUuidFromValidId() {
    UUID id = UUID.fromString("dc6b0c1b-1d0f-4f9f-8f4e-1b0f4f9f8f4e");

    assertThat(new OrdreDeFabricationId(id).uuid()).isEqualTo(id);
  }
}
