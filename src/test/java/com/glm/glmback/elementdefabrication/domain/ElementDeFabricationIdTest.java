package com.glm.glmback.elementdefabrication.domain;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@UnitTest
class ElementDeFabricationIdTest {

  @Test
  void shouldNotBuildWithoutId() {
    assertThatThrownBy(() -> new ElementDeFabricationId(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("id");
  }

  @Test
  void shouldGenerateNewId() {
    assertThat(ElementDeFabricationId.newId()).isNotEqualTo(ElementDeFabricationId.newId());
  }

  @Test
  void shouldGetUuidFromValidId() {
    UUID id = UUID.fromString("dc6b0c1b-1d0f-4f9f-8f4e-1b0f4f9f8f4e");

    assertThat(new ElementDeFabricationId(id).uuid()).isEqualTo(id);
  }

  @Test
  void shouldOrderIdsByUuid() {
    ElementDeFabricationId premier = new ElementDeFabricationId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    ElementDeFabricationId second = new ElementDeFabricationId(UUID.fromString("00000000-0000-0000-0000-000000000002"));

    assertThat(premier).isLessThan(second);
    assertThat(second).isGreaterThan(premier);
    assertThat(premier).isEqualByComparingTo(new ElementDeFabricationId(premier.uuid()));
  }
}
