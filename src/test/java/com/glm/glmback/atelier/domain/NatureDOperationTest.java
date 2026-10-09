package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.StringTooLongException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@UnitTest
class NatureDOperationTest {

  @Test
  void shouldNotBuildWithoutId() {
    assertThatThrownBy(() -> new NatureDOperation(null, "fraisage"))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("id de la nature de l'operation");
  }

  @Test
  void shouldNotBuildIdWithoutUuid() {
    assertThatThrownBy(() -> new NatureDOperationId(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("id de la nature de l'operation");
  }

  @Test
  void shouldNotBuildWithoutLibelle() {
    assertThatThrownBy(() -> new NatureDOperation(NATURE_D_OPERATION_ID_FRAISAGE, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("nature de l'operation");
  }

  @Test
  void shouldNotBuildWithBlankLibelle() {
    assertThatThrownBy(() -> new NatureDOperation(NATURE_D_OPERATION_ID_FRAISAGE, " "))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("nature de l'operation");
  }

  @Test
  void shouldNotBuildWithTooLongLibelle() {
    String tooLong = "a".repeat(51);

    assertThatThrownBy(() -> new NatureDOperation(NATURE_D_OPERATION_ID_FRAISAGE, tooLong))
      .isExactlyInstanceOf(StringTooLongException.class)
      .hasMessageContaining("nature de l'operation");
  }

  @Test
  void shouldBuildNatureDOperation() {
    NatureDOperation nature = new NatureDOperation(
      new NatureDOperationId(UUID.fromString("3e9b2f10-6c4d-4a7e-8b15-2d0f1a9c7e01")),
      "fraisage"
    );

    assertThat(nature.id()).isEqualTo(NATURE_D_OPERATION_ID_FRAISAGE);
    assertThat(nature.libelle()).isEqualTo("fraisage");
  }
}
