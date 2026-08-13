package com.glm.glmback.operateur.domain;

import static com.glm.glmback.operateur.domain.OperateursFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import org.junit.jupiter.api.Test;

@UnitTest
class PosteHabilitableIdTest {

  @Test
  void shouldNotBuildWithoutUuid() {
    assertThatThrownBy(() -> new PosteHabilitableId(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("id du poste");
  }

  @Test
  void shouldOrderIdsToMakeUnknownPosteRefusalDeterministic() {
    assertThat(ID_TOUR_1).isLessThan(ID_POSTE_DE_SOUDURE);
    assertThat(ID_POSTE_DE_SOUDURE).isGreaterThan(ID_TOUR_1);
  }
}
