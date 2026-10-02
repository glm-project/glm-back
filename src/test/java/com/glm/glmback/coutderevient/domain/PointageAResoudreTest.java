package com.glm.glmback.coutderevient.domain;

import static com.glm.glmback.coutderevient.domain.CoutDeRevientFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@UnitTest
class PointageAResoudreTest {

  @Test
  void shouldNotBuildWithoutActivite() {
    assertThatThrownBy(() -> new PointageAResoudre(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("activite a resoudre");
  }

  @Test
  void shouldExposeItsStartAndItsLatestPossibleEnd() {
    ActiviteInterpretee aResoudre = ActiviteInterpretee.builder()
      .id(new ActiviteId(UUID.randomUUID()))
      .activite(ACTIVITE_FRAISAGE)
      .plage(new Plage(LE_11_MAI_A_9H, Optional.empty()))
      .echeance(LE_11_MAI_A_21H)
      .finAuPlusTard(Optional.of(LE_11_MAI_A_11H));

    PointageAResoudre pointage = new PointageAResoudre(aResoudre);

    assertThat(pointage.activite()).isEqualTo(ACTIVITE_FRAISAGE);
    assertThat(pointage.debut()).isEqualTo(LE_11_MAI_A_9H);
    assertThat(pointage.finAuPlusTard()).contains(LE_11_MAI_A_11H);
  }
}
