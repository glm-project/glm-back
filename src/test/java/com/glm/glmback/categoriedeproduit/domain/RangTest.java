package com.glm.glmback.categoriedeproduit.domain;

import static com.glm.glmback.categoriedeproduit.domain.CategoriesDeProduitFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.NumberValueTooLowException;
import org.junit.jupiter.api.Test;

@UnitTest
class RangTest {

  @Test
  void shouldNotBuildBelowOne() {
    assertThatThrownBy(() -> new Rang(0))
      .isExactlyInstanceOf(NumberValueTooLowException.class)
      .hasMessageContaining("rang");
  }

  @Test
  void shouldStartAtOne() {
    assertThat(Rang.premier()).isEqualTo(new Rang(1));
  }

  @Test
  void shouldFollowWithNextValue() {
    assertThat(RANG_1.suivant()).isEqualTo(new Rang(2));
  }
}
