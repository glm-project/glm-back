package com.glm.glmback.coutderevient.domain;

import static com.glm.glmback.coutderevient.domain.CoutDeRevientFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import org.junit.jupiter.api.Test;

@UnitTest
class EvenementDePresenceTest {

  @Test
  void shouldNotBuildWithoutType() {
    assertThatThrownBy(() -> new EvenementDePresence(null, LE_11_MAI_A_8H))
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
  void shouldBuildWithTypeAndDate() {
    assertThat(arriveeA(LE_11_MAI_A_8H).dateDeSurvenue()).isEqualTo(LE_11_MAI_A_8H);
  }
}
