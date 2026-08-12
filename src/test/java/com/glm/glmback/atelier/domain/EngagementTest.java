package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import org.junit.jupiter.api.Test;

@UnitTest
class EngagementTest {

  @Test
  void shouldNotBuildWithoutAuteur() {
    assertThatThrownBy(() -> new Engagement(null, LE_10_MAI_2026_A_7H))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("auteur");
  }

  @Test
  void shouldNotBuildWithoutDate() {
    assertThatThrownBy(() -> new Engagement(AUTEUR_LEROY, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("date d'engagement");
  }

  @Test
  void shouldTraceQuiAEngageEtQuand() {
    assertThat(engagementParLeroy().auteur()).isEqualTo(AUTEUR_LEROY);
    assertThat(engagementParLeroy().date()).isEqualTo(LE_10_MAI_2026_A_7H);
  }
}
