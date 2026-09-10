package com.glm.glmback.syntheseheures.infrastructure.secondary;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

@UnitTest
class FuseauHoraireDeLaSyntheseTest {

  @Test
  void shouldLireLesJoursAParis() {
    assertThat(new FuseauHoraireDeLaSynthese().zone()).isEqualTo(ZoneId.of("Europe/Paris"));
  }
}
