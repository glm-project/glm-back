package com.glm.glmback.parametrage.domain;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.NumberValueTooLowException;
import org.junit.jupiter.api.Test;

@UnitTest
class ImageLueTest {

  @Test
  void shouldNotBuildWithoutFormat() {
    assertThatThrownBy(() -> new ImageLue(" ", 50, 50))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("format");
  }

  @Test
  void shouldNotBuildWithNegativeLargeur() {
    assertThatThrownBy(() -> new ImageLue("png", -1, 50))
      .isExactlyInstanceOf(NumberValueTooLowException.class)
      .hasMessageContaining("largeur");
  }

  @Test
  void shouldNotBuildWithNegativeHauteur() {
    assertThatThrownBy(() -> new ImageLue("png", 50, -1))
      .isExactlyInstanceOf(NumberValueTooLowException.class)
      .hasMessageContaining("hauteur");
  }
}
