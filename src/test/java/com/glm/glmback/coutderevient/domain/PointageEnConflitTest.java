package com.glm.glmback.coutderevient.domain;

import static com.glm.glmback.coutderevient.domain.CoutDeRevientFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@UnitTest
class PointageEnConflitTest {

  private static final UUID EVENEMENT = UUID.fromString("77777777-7777-7777-7777-777777777777");

  @Test
  void shouldNotBuildWithoutEvenement() {
    assertThatThrownBy(() -> new PointageEnConflit(null, TypeDePointage.DEBUT, LE_11_MAI_A_9H))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("evenement");
  }

  @Test
  void shouldNotBuildWithoutType() {
    assertThatThrownBy(() -> new PointageEnConflit(EVENEMENT, null, LE_11_MAI_A_9H))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("type du pointage");
  }

  @Test
  void shouldNotBuildWithoutSurvenue() {
    assertThatThrownBy(() -> new PointageEnConflit(EVENEMENT, TypeDePointage.DEBUT, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("date de survenue");
  }
}
