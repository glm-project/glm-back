package com.glm.glmback.feuilledetemps.domain;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

@UnitTest
class EvenementDePresenceTest {

  private static final Instant A_8H = Instant.parse("2026-05-11T06:00:00Z");

  @Test
  void shouldNotBuildWithoutType() {
    assertThatThrownBy(() -> new EvenementDePresence(null, A_8H))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("type");
  }

  @Test
  void shouldNotBuildWithoutDateDeSurvenue() {
    assertThatThrownBy(() -> new EvenementDePresence(TypeDEvenementDePresence.ARRIVEE, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("date de survenue");
  }

  @Test
  void shouldPorterSonTypeEtSaDate() {
    EvenementDePresence arrivee = new EvenementDePresence(TypeDEvenementDePresence.ARRIVEE, A_8H);

    assertThat(arrivee.type()).isEqualTo(TypeDEvenementDePresence.ARRIVEE);
    assertThat(arrivee.dateDeSurvenue()).isEqualTo(A_8H);
  }
}
