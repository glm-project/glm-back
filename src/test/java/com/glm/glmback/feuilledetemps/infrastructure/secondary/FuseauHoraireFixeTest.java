package com.glm.glmback.feuilledetemps.infrastructure.secondary;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

@UnitTest
class FuseauHoraireFixeTest {

  @Test
  void shouldLireLesJoursAParis() {
    assertThat(new FuseauHoraireFixe().zone()).isEqualTo(ZoneId.of("Europe/Paris"));
  }
}
