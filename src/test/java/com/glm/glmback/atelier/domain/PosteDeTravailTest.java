package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.StringTooLongException;
import org.junit.jupiter.api.Test;

@UnitTest
class PosteDeTravailTest {

  @Test
  void shouldNotBuildWithoutValue() {
    assertThatThrownBy(() -> new PosteDeTravail(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("poste de travail");
  }

  @Test
  void shouldNotBuildWithBlankValue() {
    assertThatThrownBy(() -> new PosteDeTravail(" "))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("poste de travail");
  }

  @Test
  void shouldNotBuildWithTooLongValue() {
    String tooLong = "a".repeat(100 + 1);

    assertThatThrownBy(() -> new PosteDeTravail(tooLong))
      .isExactlyInstanceOf(StringTooLongException.class)
      .hasMessageContaining("poste de travail");
  }

  @Test
  void shouldGetValueFromValidPosteDeTravail() {
    assertThat(POSTE_FRAISEUSE_1.value()).isEqualTo("fraiseuse 1");
  }
}
