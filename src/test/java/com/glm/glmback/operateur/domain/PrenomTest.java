package com.glm.glmback.operateur.domain;

import static com.glm.glmback.operateur.domain.OperateursFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.StringTooLongException;
import org.junit.jupiter.api.Test;

@UnitTest
class PrenomTest {

  @Test
  void shouldNotBuildWithoutValue() {
    assertThatThrownBy(() -> new Prenom(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("prenom");
  }

  @Test
  void shouldNotBuildWithBlankValue() {
    assertThatThrownBy(() -> new Prenom(" "))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("prenom");
  }

  @Test
  void shouldNotBuildWithTooLongValue() {
    String tooLong = "a".repeat(101);

    assertThatThrownBy(() -> new Prenom(tooLong))
      .isExactlyInstanceOf(StringTooLongException.class)
      .hasMessageContaining("prenom");
  }

  @Test
  void shouldGetValueFromValidPrenom() {
    assertThat(PRENOM_JEAN.value()).isEqualTo("Jean");
  }
}
