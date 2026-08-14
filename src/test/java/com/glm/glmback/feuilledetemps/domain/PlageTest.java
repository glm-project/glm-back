package com.glm.glmback.feuilledetemps.domain;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.NotAfterTimeException;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class PlageTest {

  private static final Instant A_8H = Instant.parse("2026-05-11T06:00:00Z");
  private static final Instant A_12H = Instant.parse("2026-05-11T10:00:00Z");

  @Test
  void shouldNotBuildWithoutDebut() {
    assertThatThrownBy(() -> new Plage(null, Optional.empty()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("debut");
  }

  @Test
  void shouldNotBuildWithoutFin() {
    assertThatThrownBy(() -> new Plage(A_8H, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("fin");
  }

  @Test
  void shouldNotBuildWithFinBeforeDebut() {
    assertThatThrownBy(() -> new Plage(A_12H, Optional.of(A_8H)))
      .isExactlyInstanceOf(NotAfterTimeException.class)
      .hasMessageContaining("fin");
  }

  @Test
  void shouldBeOuverteWithoutFin() {
    assertThat(new Plage(A_8H, Optional.empty()).estOuverte()).isTrue();
  }

  @Test
  void shouldNotBeOuverteWithFin() {
    assertThat(new Plage(A_8H, Optional.of(A_12H)).estOuverte()).isFalse();
  }
}
