package com.glm.glmback.elementdefabrication.domain;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@UnitTest
class ProduitIdTest {

  @Test
  void shouldNotBuildWithoutId() {
    assertThatThrownBy(() -> new ProduitId(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("id");
  }

  @Test
  void shouldGenerateNewId() {
    assertThat(ProduitId.newId()).isNotEqualTo(ProduitId.newId());
  }

  @Test
  void shouldGetUuidFromValidId() {
    UUID id = UUID.fromString("dc6b0c1b-1d0f-4f9f-8f4e-1b0f4f9f8f4e");

    assertThat(new ProduitId(id).uuid()).isEqualTo(id);
  }
}
