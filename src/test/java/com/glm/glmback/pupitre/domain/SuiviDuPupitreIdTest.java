package com.glm.glmback.pupitre.domain;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@UnitTest
class SuiviDuPupitreIdTest {

  @Test
  void shouldNotBuildWithoutUuid() {
    assertThatThrownBy(() -> new SuiviDuPupitreId(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("id du suivi");
  }

  @Test
  void shouldBuildWithUuid() {
    UUID uuid = UUID.fromString("11111111-1111-1111-1111-111111111111");

    assertThat(new SuiviDuPupitreId(uuid).uuid()).isEqualTo(uuid);
  }
}
