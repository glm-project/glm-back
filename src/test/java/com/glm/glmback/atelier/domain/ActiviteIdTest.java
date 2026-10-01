package com.glm.glmback.atelier.domain;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@UnitTest
class ActiviteIdTest {

  @Test
  void shouldNotBuildWithoutUuid() {
    assertThatThrownBy(() -> new ActiviteId(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("id de l'activite");
  }

  @Test
  void shouldPrendreLIdentiteDuPointageQuiOuvreLActivite() {
    EvenementDAtelierId ouvrant = new EvenementDAtelierId(UUID.fromString("00000000-0000-0000-0000-000000000042"));

    assertThat(ActiviteId.ouvertePar(ouvrant)).isEqualTo(new ActiviteId(ouvrant.uuid()));
  }
}
