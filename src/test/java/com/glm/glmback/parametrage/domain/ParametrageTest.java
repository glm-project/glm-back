package com.glm.glmback.parametrage.domain;

import static com.glm.glmback.parametrage.domain.ParametrageFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.time.Duration;
import org.junit.jupiter.api.Test;

@UnitTest
class ParametrageTest {

  @Test
  void shouldNotBuildWithoutDureeMaxDActivite() {
    assertThatThrownBy(() -> new Parametrage(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("dureeMaxDActivite");
  }

  @Test
  void shouldFixeLaDureeMaxDActivite() {
    Parametrage parametrage = new Parametrage(DureeMaxDActivite.parDefaut());

    assertThat(parametrage.fixeLaDureeMaxDActivite(DUREE_MAX_D_ACTIVITE_DIX_HEURES).dureeMaxDActivite()).isEqualTo(
      new DureeMaxDActivite(Duration.ofHours(10))
    );
  }
}
