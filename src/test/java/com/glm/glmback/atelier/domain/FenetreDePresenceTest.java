package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.NotAfterTimeException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class FenetreDePresenceTest {

  @Test
  void shouldNotBuildWithoutDebut() {
    assertThatThrownBy(() -> new FenetreDePresence(null, Optional.empty()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("debut");
  }

  @Test
  void shouldNotBuildWithoutFin() {
    assertThatThrownBy(() -> new FenetreDePresence(LE_10_MAI_2026_A_8H, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("fin");
  }

  @Test
  void shouldNotBuildWithFinBeforeDebut() {
    assertThatThrownBy(() -> new FenetreDePresence(LE_10_MAI_2026_A_9H, Optional.of(LE_10_MAI_2026_A_8H)))
      .isExactlyInstanceOf(NotAfterTimeException.class)
      .hasMessageContaining("fin");
  }

  @Test
  void shouldBeOuverteWithoutFin() {
    assertThat(new FenetreDePresence(LE_10_MAI_2026_A_8H, Optional.empty()).estOuverte()).isTrue();
  }

  @Test
  void shouldNotBeOuverteWithFin() {
    assertThat(matinee().estOuverte()).isFalse();
  }

  private static FenetreDePresence matinee() {
    return new FenetreDePresence(LE_10_MAI_2026_A_8H, Optional.of(LE_10_MAI_2026_A_12H));
  }
}
