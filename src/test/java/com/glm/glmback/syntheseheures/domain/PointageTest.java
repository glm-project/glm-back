package com.glm.glmback.syntheseheures.domain;

import static com.glm.glmback.syntheseheures.domain.SyntheseHeuresFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import org.junit.jupiter.api.Test;

@UnitTest
class PointageTest {

  @Test
  void shouldNotBuildWithoutEvenement() {
    assertThatThrownBy(() -> new Pointage(null, true))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("evenement");
  }

  @Test
  void shouldPorterSonEvenementEtSaValidite() {
    EvenementDePresence arrivee = arriveeA(LE_LUNDI_11_MAI_2026_A_8H);

    Pointage pointage = new Pointage(arrivee, true);

    assertThat(pointage.evenement()).isEqualTo(arrivee);
    assertThat(pointage.valide()).isTrue();
  }

  @Test
  void shouldPorterUnPointageInvalide() {
    EvenementDePresence pause = pauseA(LE_LUNDI_11_MAI_2026_A_12H);

    assertThat(new Pointage(pause, false).valide()).isFalse();
  }
}
